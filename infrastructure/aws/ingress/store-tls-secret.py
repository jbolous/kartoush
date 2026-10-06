#!/usr/bin/env python3
"""Export an issued ACM certificate into Secrets Manager without printing private material."""

import argparse
import datetime
import json
import os
import secrets
import subprocess
import sys

import boto3
from botocore.exceptions import BotoCoreError, ClientError

REGION = "us-east-2"
PROJECT_ID = "790873128308"
HOSTNAME = "api.kartoush.dev"
SECRET_NAME = "/kartoush/demo/tls"


def store_bundle(session, certificate_arn, rotate=False):
    if session.client("sts").get_caller_identity()["Account"] != PROJECT_ID:
        raise RuntimeError("Wrong AWS project")
    if not certificate_arn.startswith(f"arn:aws:acm:{REGION}:{PROJECT_ID}:certificate/"):
        raise RuntimeError("Certificate must belong to the Ohio demo project")
    acm = session.client("acm")
    sm = session.client("secretsmanager")
    certificate = acm.describe_certificate(CertificateArn=certificate_arn)["Certificate"]
    if certificate["Status"] != "ISSUED" or certificate.get("Options", {}).get("Export") != "ENABLED":
        raise RuntimeError("Certificate must be issued and exportable")
    if set(certificate.get("SubjectAlternativeNames", [])) != {HOSTNAME}:
        raise RuntimeError("Certificate must cover only the selected API hostname")
    if certificate["NotAfter"] <= datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(hours=1):
        raise RuntimeError("Certificate expires too soon")
    existing = None
    try:
        existing = sm.describe_secret(SecretId=SECRET_NAME)
    except sm.exceptions.ResourceNotFoundException:
        pass
    if existing is not None and not rotate:
        raise RuntimeError("TLS secret exists; use --rotate only for deliberate certificate refresh")

    passphrase = secrets.token_urlsafe(48)
    exported = acm.export_certificate(CertificateArn=certificate_arn, Passphrase=passphrase.encode())
    decrypted = subprocess.run(
        ["openssl", "pkey", "-passin", "env:ACM_EXPORT_PASSPHRASE"],
        input=exported["PrivateKey"], capture_output=True, text=True,
        env={"PATH": os.environ.get("PATH", ""), "ACM_EXPORT_PASSPHRASE": passphrase},
        timeout=15,
    )
    if decrypted.returncode:
        raise RuntimeError("Private-key decryption failed; no secret updated")
    values = json.dumps({
        "certificate_pem": exported["Certificate"],
        "certificate_chain_pem": exported["CertificateChain"],
        "private_key_pem": decrypted.stdout,
        "certificate_arn": certificate_arn,
        "not_after": certificate["NotAfter"].isoformat(),
    })
    if existing is not None:
        result = sm.put_secret_value(SecretId=existing["ARN"], SecretString=values)
    else:
        result = sm.create_secret(
            Name=SECRET_NAME, Description="Exported ACM TLS bundle for the demo HTTPS proxy",
            SecretString=values,
            Tags=[{"Key": "Project", "Value": "kartoush"}, {"Key": "Environment", "Value": "demo"}, {"Key": "Name", "Value": SECRET_NAME}],
        )
    return {"secretArn": result["ARN"], "versionId": result["VersionId"], "certificateArn": certificate_arn, "notAfter": certificate["NotAfter"].isoformat()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", default="kartoush")
    parser.add_argument("--certificate-arn", required=True)
    parser.add_argument("--rotate", action="store_true")
    args = parser.parse_args()
    try:
        session = boto3.Session(profile_name=args.profile, region_name=REGION)
        print(json.dumps(store_bundle(session, args.certificate_arn, args.rotate), indent=2))
        return 0
    except ClientError as error:
        print("AWS request failed: " + error.response["Error"]["Code"], file=sys.stderr)
        return 1
    except BotoCoreError as error:
        print("AWS SDK failure: " + type(error).__name__, file=sys.stderr)
        return 1
    except (OSError, subprocess.TimeoutExpired):
        print("OpenSSL could not complete private-key decryption", file=sys.stderr)
        return 1
    except RuntimeError as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
