"""Focused regressions for provisioning after a partial ECS failure."""

import importlib.util
import json
from pathlib import Path
import unittest
from unittest.mock import MagicMock

ROOT = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("provision_service", ROOT / "provision-service.py")
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class ProvisionServiceTest(unittest.TestCase):
    def setUp(self):
        self.config = json.loads((ROOT / "cluster.json").read_text())
        self.cluster = {
            **self.config,
            "clusterArn": "arn:aws:ecs:us-east-2:790873128308:cluster/kartoush-demo-cluster",
            "status": "ACTIVE",
        }
        self.ecs = MagicMock()
        self.ec2 = MagicMock()
        identity = MagicMock()
        identity.get_caller_identity.return_value = {"Account": MODULE.PROJECT_ID}
        self.session = MagicMock()
        self.session.client.side_effect = {"sts": identity, "ecs": self.ecs, "ec2": self.ec2}.__getitem__
        self.ecs.describe_clusters.return_value = {"clusters": [self.cluster], "failures": []}
        self.ecs.describe_services.return_value = {"services": [], "failures": [{"reason": "MISSING"}]}
        tags = [{"Key": "Project", "Value": "kartoush"}, {"Key": "Environment", "Value": "demo"}]
        self.ec2.describe_subnets.return_value = {"Subnets": [{"SubnetId": "subnet-test", "VpcId": "vpc-demo", "Tags": tags}]}
        self.ec2.describe_security_groups.return_value = {"SecurityGroups": [{"GroupName": "kartoush-demo-app", "VpcId": "vpc-demo", "Tags": tags}]}
        self.ec2.describe_route_tables.return_value = {"RouteTables": [{"Routes": [{"DestinationCidrBlock": "0.0.0.0/0", "GatewayId": "igw-test", "State": "active"}]}]}
        self.ecs.create_cluster.return_value = {"cluster": self.cluster}
        self.ecs.register_task_definition.return_value = {"taskDefinition": {"taskDefinitionArn": "revision-2"}}
        self.ecs.create_service.return_value = {"service": {"serviceArn": "service-test", "desiredCount": 0, "runningCount": 0, "pendingCount": 0}}

    def test_reuses_active_cluster_after_partial_failure(self):
        result = MODULE.provision(self.session)
        self.ecs.create_cluster.assert_not_called()
        self.ecs.describe_clusters.assert_called_once_with(clusters=[self.config["clusterName"]], include=["TAGS", "SETTINGS"])
        self.assertEqual(result["clusterArn"], self.cluster["clusterArn"])
        self.assertEqual(self.ecs.create_service.call_args.kwargs["taskDefinition"], "revision-2")
        self.assertEqual(self.ecs.create_service.call_args.kwargs["desiredCount"], 0)

    def test_creates_missing_cluster(self):
        self.ecs.describe_clusters.return_value = {"clusters": [], "failures": [{"reason": "MISSING"}]}
        MODULE.provision(self.session)
        self.ecs.create_cluster.assert_called_once_with(**self.config)
        self.ecs.describe_services.assert_not_called()

    def test_refuses_unsafe_cluster_reuse_before_mutation(self):
        for change in ({"tags": []}, {"settings": [{"name": "containerInsights", "value": "enabled"}]}, {"status": "PROVISIONING"}):
            with self.subTest(change=change):
                self.ecs.describe_clusters.return_value = {"clusters": [{**self.cluster, **change}]}
                with self.assertRaises(RuntimeError):
                    MODULE.provision(self.session)
                self.ecs.create_cluster.assert_not_called()
                self.ecs.register_task_definition.assert_not_called()
                self.ecs.create_service.assert_not_called()

    def test_refuses_existing_service_before_mutation(self):
        self.ecs.describe_services.return_value = {"services": [{"status": "ACTIVE"}], "failures": []}
        with self.assertRaisesRegex(RuntimeError, "Service already exists"):
            MODULE.provision(self.session)
        self.ecs.create_cluster.assert_not_called()
        self.ecs.register_task_definition.assert_not_called()
        self.ecs.create_service.assert_not_called()


if __name__ == "__main__":
    unittest.main()
