#!/usr/bin/env python3
"""Create the inactive demo ECS service; never start tasks or update a live service."""

import argparse
import json
from pathlib import Path
import sys

import boto3
from botocore.exceptions import BotoCoreError, ClientError

REGION = "us-east-2"
PROJECT_ID = "790873128308"
ROOT = Path(__file__).resolve().parent


def provision(session):
    if session.client("sts").get_caller_identity()["Account"] != PROJECT_ID:
        raise RuntimeError("Wrong AWS project; refusing to provision")

    ecs = session.client("ecs")
    ec2 = session.client("ec2")
    cluster = json.loads((ROOT / "cluster.json").read_text())
    service = json.loads((ROOT / "service.json").read_text())
    task = json.loads((ROOT / "task-definition.json").read_text())
    if service["desiredCount"] != 0:
        raise RuntimeError("Provisioning must preserve the inactive baseline")

    existing_cluster = ecs.describe_clusters(clusters=[cluster["clusterName"]])
    existing = {"services": [], "failures": []}
    if any(c["status"] == "ACTIVE" for c in existing_cluster.get("clusters", [])):
        existing = ecs.describe_services(
            cluster=cluster["clusterName"], services=[service["serviceName"]]
        )
    if any(s["status"] != "INACTIVE" for s in existing.get("services", [])):
        raise RuntimeError("Service already exists; inspect it before changing configuration")
    if any(f["reason"] not in ("MISSING",) for f in existing.get("failures", [])):
        raise RuntimeError("Cannot verify existing service state")

    network = service["networkConfiguration"]["awsvpcConfiguration"]
    subnets = ec2.describe_subnets(SubnetIds=network["subnets"])["Subnets"]
    groups = ec2.describe_security_groups(GroupIds=network["securityGroups"])["SecurityGroups"]
    vpcs = {s["VpcId"] for s in subnets} | {g["VpcId"] for g in groups}
    if len(vpcs) != 1:
        raise RuntimeError("Subnets and security group must share the demo VPC")
    for resource in subnets + groups:
        tags = {t["Key"]: t["Value"] for t in resource.get("Tags", [])}
        if tags.get("Project") != "kartoush" or tags.get("Environment") != "demo":
            raise RuntimeError("Network resource does not belong to the demo environment")
    if any(g["GroupName"] != "kartoush-demo-app" for g in groups):
        raise RuntimeError("Unexpected application security group")
    for subnet in subnets:
        tables = ec2.describe_route_tables(
            Filters=[{"Name": "association.subnet-id", "Values": [subnet["SubnetId"]]}]
        )["RouteTables"]
        if not tables:
            tables = ec2.describe_route_tables(Filters=[
                {"Name": "vpc-id", "Values": [subnet["VpcId"]]},
                {"Name": "association.main", "Values": ["true"]},
            ])["RouteTables"]
        if not any(r.get("DestinationCidrBlock") == "0.0.0.0/0"
                   and r.get("GatewayId", "").startswith("igw-")
                   and r.get("State") == "active"
                   for table in tables for r in table["Routes"]):
            raise RuntimeError("Application subnet must have an active Internet Gateway route")

    created = ecs.create_cluster(**cluster)["cluster"]
    definition = ecs.register_task_definition(**task)["taskDefinition"]
    service["taskDefinition"] = definition["taskDefinitionArn"]
    deployed = ecs.create_service(**service)["service"]
    return {
        "region": REGION,
        "clusterArn": created["clusterArn"],
        "serviceArn": deployed["serviceArn"],
        "taskDefinitionArn": definition["taskDefinitionArn"],
        "desiredCount": deployed["desiredCount"],
        "runningCount": deployed["runningCount"],
        "pendingCount": deployed["pendingCount"],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", default="kartoush")
    args = parser.parse_args()
    try:
        result = provision(boto3.Session(profile_name=args.profile, region_name=REGION))
        print(json.dumps(result, indent=2))
        return 0
    except ClientError as error:
        print("AWS request failed: " + error.response["Error"]["Code"], file=sys.stderr)
        return 1
    except BotoCoreError as error:
        print("AWS SDK failure: " + type(error).__name__, file=sys.stderr)
        return 1
    except RuntimeError as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
