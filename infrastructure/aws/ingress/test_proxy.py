"""Container checks for the production proxy using an ephemeral test CA and mock API."""

import http.client
import json
import os
from pathlib import Path
import socket
import ssl
import subprocess
import tempfile
import time
import unittest
import uuid

IMAGE = os.environ.get("KARTOUSH_PROXY_TEST_IMAGE", "kartoush-proxy:local")
HOST = "api.kartoush.dev"
CUSTOMER = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
BACKEND = '''from http.server import BaseHTTPRequestHandler, HTTPServer
import json
class Handler(BaseHTTPRequestHandler):
 def handle_request(self):
  body = self.rfile.read(int(self.headers.get('Content-Length', 0))).decode()
  data = json.dumps({'path': self.path, 'method': self.command, 'headers': dict(self.headers), 'body': body}).encode()
  self.send_response(200); self.send_header('Content-Type','application/json'); self.send_header('Content-Length',str(len(data))); self.end_headers()
  if self.command != 'HEAD': self.wfile.write(data)
 do_GET = do_HEAD = do_POST = do_PUT = do_DELETE = do_PATCH = do_OPTIONS = handle_request
 def log_message(self, *args): pass
HTTPServer(('127.0.0.1',8080),Handler).serve_forever()
'''


def command(*args, env=None):
    result = subprocess.run(args, env=env, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(f"Command failed: {args[0]} {args[1]}: {result.stderr[:1000]}")
    return result.stdout.strip()


class ProxyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory(prefix="kartoush-ingress-")
        cls.root = Path(cls.temp.name)
        cls.containers = []
        cls.volume = "kartoush-ingress-test-" + uuid.uuid4().hex
        cls.addClassCleanup(cls.cleanup)
        command("docker", "volume", "create", cls.volume)
        root = cls.root
        command("openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "2", "-subj", "/CN=Kartoush Test CA", "-addext", "basicConstraints=critical,CA:TRUE", "-addext", "keyUsage=critical,keyCertSign,cRLSign", "-keyout", str(root / "ca.key"), "-out", str(root / "ca.pem"))
        command("openssl", "req", "-new", "-newkey", "rsa:2048", "-nodes", "-subj", "/CN=" + HOST, "-keyout", str(root / "key.pem"), "-out", str(root / "server.csr"))
        (root / "extensions").write_text("subjectAltName=DNS:" + HOST + "\nbasicConstraints=CA:FALSE\nextendedKeyUsage=serverAuth\nkeyUsage=digitalSignature,keyEncipherment\n")
        command("openssl", "x509", "-req", "-in", str(root / "server.csr"), "-CA", str(root / "ca.pem"), "-CAkey", str(root / "ca.key"), "-CAcreateserial", "-days", "1", "-extfile", str(root / "extensions"), "-out", str(root / "certificate.pem"))
        # Host and Docker VM clocks can briefly straddle the issuance second.
        time.sleep(2)
        tls_env = {**os.environ, "TLS_CERTIFICATE_PEM": (root / "certificate.pem").read_text(), "TLS_CERTIFICATE_CHAIN_PEM": (root / "ca.pem").read_text(), "TLS_PRIVATE_KEY_PEM": (root / "key.pem").read_text()}
        cls.tls_env = tls_env
        command("docker", "run", "--rm", "--platform", "linux/amd64", "--user", "0", "--network", "none", "-v", cls.volume + ":/tls", "-v", str(root / "ca.pem") + ":/etc/ssl/certs/ca-certificates.crt:ro", "-e", "TLS_CERTIFICATE_PEM", "-e", "TLS_CERTIFICATE_CHAIN_PEM", "-e", "TLS_PRIVATE_KEY_PEM", "--entrypoint", "bash", IMAGE, "/opt/kartoush/materialize-tls.sh", env=tls_env)
        permissions = command("docker", "run", "--rm", "--platform", "linux/amd64", "-v", cls.volume + ":/tls:ro", "--entrypoint", "stat", IMAGE, "-c", "%a:%u:%g", "/tls/private-key.pem")
        if permissions != "400:10001:10001":
            raise RuntimeError("TLS key permissions are incorrect")
        command("docker", "run", "--rm", "--platform", "linux/amd64", "-v", cls.volume + ":/tls:ro", "--entrypoint", "nginx", IMAGE, "-t")
        (root / "backend.py").write_text(BACKEND)
        backend = command("docker", "run", "-d", "--platform", "linux/amd64", "-p", "127.0.0.1::443", "-v", str(root / "backend.py") + ":/backend.py:ro", "python:3.14-slim", "python", "/backend.py")
        cls.containers.append(backend)
        port = command("docker", "port", backend, "443/tcp")
        cls.port = int(port.rsplit(":", 1)[1])
        proxy = command("docker", "run", "-d", "--platform", "linux/amd64", "--network", "container:" + backend, "--memory", "128m", "-v", cls.volume + ":/tls:ro", IMAGE)
        cls.containers.append(proxy)
        cls.context = ssl.create_default_context(cafile=str(root / "ca.pem"))
        for _ in range(60):
            try:
                cls.request("GET", "/api/terms-of-service/current")
                break
            except (OSError, http.client.HTTPException) as error:
                last_error = repr(error)
                time.sleep(0.5)
        else:
            raise RuntimeError("Proxy did not start: " + last_error + "; state=" + command("docker", "inspect", "--format", "{{json .State}}", proxy) + "; logs=" + command("docker", "logs", proxy))

    @classmethod
    def cleanup(cls):
        for container in reversed(cls.containers):
            subprocess.run(["docker", "rm", "-f", container], capture_output=True)
        subprocess.run(["docker", "volume", "rm", cls.volume], capture_output=True)
        cls.temp.cleanup()

    @classmethod
    def request(cls, method, path, *, host=HOST, headers=None, body=None, sni=HOST):
        raw = socket.create_connection(("127.0.0.1", cls.port), timeout=5)
        try:
            tls = cls.context.wrap_socket(raw, server_hostname=sni)
        except Exception:
            raw.close()
            raise
        conn = http.client.HTTPConnection("127.0.0.1", cls.port, timeout=5)
        conn.sock = tls
        try:
            conn.request(method, path, body=body, headers={"Host": host, **(headers or {})})
            response = conn.getresponse()
            return response.status, dict(response.getheaders()), response.read()
        finally:
            conn.close()

    def test_initializer_refuses_bad_tls_material(self):
        root = self.root
        command("openssl", "genpkey", "-algorithm", "RSA", "-pkeyopt", "rsa_keygen_bits:2048", "-out", str(root / "wrong.key"))
        (root / "wrong-host.extensions").write_text("subjectAltName=DNS:other.example\nbasicConstraints=CA:FALSE\nextendedKeyUsage=serverAuth\n")
        command("openssl", "x509", "-req", "-in", str(root / "server.csr"), "-CA", str(root / "ca.pem"), "-CAkey", str(root / "ca.key"), "-days", "1", "-extfile", str(root / "wrong-host.extensions"), "-out", str(root / "wrong-host.pem"))
        command("openssl", "x509", "-req", "-in", str(root / "server.csr"), "-CA", str(root / "ca.pem"), "-CAkey", str(root / "ca.key"), "-days", "0", "-extfile", str(root / "extensions"), "-out", str(root / "expired.pem"))
        volume = "kartoush-ingress-test-" + uuid.uuid4().hex
        command("docker", "volume", "create", volume)
        try:
            for change in ({"TLS_PRIVATE_KEY_PEM": (root / "wrong.key").read_text()}, {"TLS_CERTIFICATE_PEM": (root / "wrong-host.pem").read_text()}, {"TLS_CERTIFICATE_PEM": (root / "expired.pem").read_text()}):
                with self.subTest(field=next(iter(change))):
                    result = subprocess.run(["docker", "run", "--rm", "--platform", "linux/amd64", "--user", "0", "--network", "none", "-v", volume + ":/tls", "-v", str(root / "ca.pem") + ":/etc/ssl/certs/ca-certificates.crt:ro", "-e", "TLS_CERTIFICATE_PEM", "-e", "TLS_CERTIFICATE_CHAIN_PEM", "-e", "TLS_PRIVATE_KEY_PEM", "--entrypoint", "bash", IMAGE, "/opt/kartoush/materialize-tls.sh"], env={**self.tls_env, **change}, capture_output=True)
                    self.assertNotEqual(result.returncode, 0)
        finally:
            subprocess.run(["docker", "volume", "rm", volume], capture_output=True)

    def test_allowed_controller_methods_and_paths(self):
        routes = [("POST", "/api/customers"), ("POST", "/api/auth/sign-in"), ("POST", "/api/auth/password-reset"), ("POST", "/api/auth/password-reset/confirm"), ("GET", "/api/terms-of-service/current"), ("HEAD", "/api/terms-of-service/1.0.0")]
        routes += [(m, "/api/customers/" + CUSTOMER) for m in ("GET", "HEAD", "PUT", "DELETE")]
        routes += [("POST", "/api/customers/" + CUSTOMER + "/" + suffix) for suffix in ("activation", "initial-password", "activation/resend")]
        for method, path in routes:
            with self.subTest(method=method, path=path):
                self.assertEqual(self.request(method, path)[0], 200)

    def test_blocks_operational_unknown_and_normalized_paths(self):
        paths = ["/internal/customers", "/dev/customers/activate", "/actuator/health", "/v3/api-docs", "/swagger-ui/index.html", "/dashboard", "/", "/api/unknown", "/api/auth/sign-in/", "/api//auth/sign-in", "/api/auth/../auth/sign-in", "/api/%61uth/sign-in", "/api/auth%2fsign-in", "/api/auth/sign-in%3bfoo", "/api/auth/sign-in/../../internal/customers", "/api/%2e%2e/internal/customers", "/api/auth/%252e%252e/sign-in"]
        for path in paths:
            with self.subTest(path=path):
                self.assertIn(self.request("POST", path)[0], (400, 404))
        for method in ("GET", "PUT", "DELETE", "PATCH", "OPTIONS", "TRACE"):
            with self.subTest(method=method):
                self.assertIn(self.request(method, "/api/auth/sign-in")[0], (404, 405))
        self.assertEqual(self.request("POST", "/api/terms-of-service/current")[0], 404)

    def test_rejects_wrong_host_and_sni(self):
        for host in ("127.0.0.1", "evil.example", HOST + ":8080"):
            with self.subTest(host=host):
                self.assertEqual(self.request("GET", "/api/terms-of-service/current", host=host)[0], 421)
        with self.assertRaises(ssl.SSLError):
            self.request("GET", "/api/terms-of-service/current", sni="evil.example")

    def test_preserves_body_auth_and_replaces_forwarding_headers(self):
        body = '{"test":"payload"}'
        status, headers, data = self.request("POST", "/api/auth/sign-in?test=value", body=body, headers={"Authorization": "Bearer test-token", "Content-Type": "application/json", "X-Forwarded-For": "203.0.113.1", "X-Forwarded-Proto": "http", "X-Forwarded-Host": "evil.example", "X-Forwarded-Port": "80", "X-Forwarded-Prefix": "/internal", "Forwarded": "host=evil.example;proto=http"})
        self.assertEqual(status, 200)
        upstream = json.loads(data)
        self.assertEqual(upstream["body"], body)
        self.assertEqual(upstream["headers"]["Authorization"], "Bearer test-token")
        self.assertEqual(upstream["headers"]["X-Forwarded-Proto"], "https")
        self.assertEqual(upstream["headers"]["X-Forwarded-Host"], HOST)
        self.assertEqual(upstream["headers"]["X-Forwarded-Port"], "443")
        self.assertNotEqual(upstream["headers"]["X-Forwarded-For"], "203.0.113.1")
        self.assertNotIn("Forwarded", upstream["headers"])
        self.assertNotIn("X-Forwarded-Prefix", upstream["headers"])
        self.assertIn("Strict-Transport-Security", headers)
        logs = command("docker", "logs", self.containers[-1])
        self.assertTrue(logs.strip(), "Proxy must deliver access logs")
        self.assertNotIn("test-token", logs)
        self.assertNotIn("test=value", logs)
        self.assertNotIn("payload", logs)


if __name__ == "__main__":
    unittest.main()
