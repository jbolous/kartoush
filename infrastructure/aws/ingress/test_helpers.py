"""Guard and metadata checks for ingress helpers; all AWS operations are mocked."""

import datetime
import importlib.util
import json
from pathlib import Path
import unittest
from unittest.mock import MagicMock, patch

ROOT = Path(__file__).resolve().parent


def load(name):
    spec = importlib.util.spec_from_file_location(name.replace('-', '_'), ROOT / (name + '.py'))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


TLS = load('store-tls-secret')
DEPLOY = load('deploy-ingress')
CERTIFICATE_ARN = 'arn:aws:acm:us-east-2:790873128308:certificate/test'
SECRET_ARN = 'arn:aws:secretsmanager:us-east-2:790873128308:secret:/kartoush/demo/tls-abcdef'
IMAGE = '790873128308.dkr.ecr.us-east-2.amazonaws.com/kartoush-demo-proxy@sha256:' + 'a' * 64


class HelpersTest(unittest.TestCase):
    def setUp(self):
        self.clients = {name: MagicMock() for name in ('sts', 'acm', 'secretsmanager', 'ecs', 'ecr', 'logs')}
        self.session = MagicMock()
        self.session.client.side_effect = self.clients.__getitem__
        self.clients['sts'].get_caller_identity.return_value = {'Account': TLS.PROJECT_ID}
        self.cert = {'Status': 'ISSUED', 'Options': {'Export': 'ENABLED'}, 'SubjectAlternativeNames': [TLS.HOSTNAME], 'NotAfter': datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=30)}
        self.clients['acm'].describe_certificate.return_value = {'Certificate': self.cert}
        self.clients['secretsmanager'].describe_secret.return_value = {'ARN': SECRET_ARN, 'Name': '/kartoush/demo/tls', 'Tags': [{'Key': 'Project', 'Value': 'kartoush'}, {'Key': 'Environment', 'Value': 'demo'}]}
        self.service = {'status': 'ACTIVE', 'desiredCount': 0, 'runningCount': 0, 'pendingCount': 0}
        self.clients['ecs'].describe_services.return_value = {'services': [self.service]}
        self.clients['ecs'].register_task_definition.return_value = {'taskDefinition': {'taskDefinitionArn': 'revision-2'}}
        self.clients['ecs'].update_service.return_value = {'service': {'serviceArn': 'service-test', 'desiredCount': 0}}

    def test_rejects_wrong_project_before_mutations(self):
        self.clients['sts'].get_caller_identity.return_value = {'Account': 'wrong'}
        with self.assertRaises(RuntimeError): TLS.store_bundle(self.session, CERTIFICATE_ARN)
        with self.assertRaises(RuntimeError): DEPLOY.deploy(self.session, IMAGE, SECRET_ARN)
        self.clients['acm'].export_certificate.assert_not_called()
        self.clients['ecs'].update_service.assert_not_called()

    def test_rejects_invalid_certificate_before_export(self):
        for change in ({'Status': 'PENDING_VALIDATION'}, {'Options': {'Export': 'DISABLED'}}, {'SubjectAlternativeNames': ['other.example']}, {'NotAfter': datetime.datetime.now(datetime.timezone.utc)}):
            with self.subTest(change=change):
                self.clients['acm'].describe_certificate.return_value = {'Certificate': {**self.cert, **change}}
                with self.assertRaises(RuntimeError): TLS.store_bundle(self.session, CERTIFICATE_ARN)
                self.clients['acm'].export_certificate.assert_not_called()

    def test_refuses_implicit_existing_secret_replacement(self):
        with self.assertRaisesRegex(RuntimeError, 'TLS secret exists'):
            TLS.store_bundle(self.session, CERTIFICATE_ARN)
        self.clients['acm'].export_certificate.assert_not_called()

    def test_rotation_keeps_private_values_out_of_return_metadata(self):
        self.clients['acm'].export_certificate.return_value = {'Certificate': 'PUBLIC_CERT', 'CertificateChain': 'PUBLIC_CHAIN', 'PrivateKey': 'ENCRYPTED_TEST_KEY'}
        self.clients['secretsmanager'].put_secret_value.return_value = {'ARN': SECRET_ARN, 'VersionId': 'new-version'}
        with patch.object(TLS.subprocess, 'run', return_value=MagicMock(returncode=0, stdout='TEST_PRIVATE_KEY')) as openssl:
            result = TLS.store_bundle(self.session, CERTIFICATE_ARN, rotate=True)
        stored = json.loads(self.clients['secretsmanager'].put_secret_value.call_args.kwargs['SecretString'])
        self.assertEqual(stored['private_key_pem'], 'TEST_PRIVATE_KEY')
        self.assertNotIn('TEST_PRIVATE_KEY', json.dumps(result))
        self.assertNotIn('TEST_PRIVATE_KEY', str(openssl.call_args.args))
        self.assertEqual(result['versionId'], 'new-version')

    def test_deployment_preserves_zero_and_limits_tls_access(self):
        DEPLOY.deploy(self.session, IMAGE, SECRET_ARN)
        task = self.clients['ecs'].register_task_definition.call_args.kwargs
        app, initializer, proxy = task['containerDefinitions']
        self.assertNotIn('mountPoints', app)
        self.assertNotIn('secrets', proxy)
        self.assertTrue(proxy['mountPoints'][0]['readOnly'])
        self.assertEqual(initializer['image'], IMAGE)
        self.assertTrue(all(v['valueFrom'].startswith(SECRET_ARN + ':') for v in initializer['secrets']))
        self.assertEqual(self.clients['ecs'].update_service.call_args.kwargs['desiredCount'], 0)

    def test_refuses_running_or_pending_service_before_mutations(self):
        for field in ('desiredCount', 'runningCount', 'pendingCount'):
            with self.subTest(field=field):
                self.clients['ecs'].describe_services.return_value = {'services': [{**self.service, field: 1}]}
                with self.assertRaises(RuntimeError): DEPLOY.deploy(self.session, IMAGE, SECRET_ARN)
                self.clients['logs'].create_log_group.assert_not_called()
                self.clients['ecs'].register_task_definition.assert_not_called()
                self.clients['ecs'].update_service.assert_not_called()


if __name__ == '__main__':
    unittest.main()
