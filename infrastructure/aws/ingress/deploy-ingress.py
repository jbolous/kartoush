#!/usr/bin/env python3
"""Install the combined HTTPS task revision while preserving an inactive ECS service."""

import argparse
import json
from pathlib import Path
import re
import sys

import boto3
from botocore.exceptions import BotoCoreError, ClientError

REGION = "us-east-2"
PROJECT_ID = "790873128308"
CLUSTER = "kartoush-demo-cluster"
SERVICE = "kartoush-demo-app"
ROOT = Path(__file__).resolve().parent


def deploy(session, proxy_image, tls_secret_arn):
    if session.client("sts").get_caller_identity()["Account"] != PROJECT_ID:
        raise RuntimeError("Wrong AWS project")
    prefix = f"{PROJECT_ID}.dkr.ecr.{REGION}.amazonaws.com/kartoush-demo-proxy@sha256:"
    if not proxy_image.startswith(prefix) or not re.fullmatch(r"[a-f0-9]{64}", proxy_image[len(prefix):]):
        raise RuntimeError("Proxy image must be a digest pinned in the Ohio demo ECR repository")
    ecs = session.client("ecs")
    response = ecs.describe_services(cluster=CLUSTER, services=[SERVICE])
    if response.get("failures") or len(response.get("services", [])) != 1:
        raise RuntimeError("Cannot verify demo service")
    service = response["services"][0]
    if service["status"] != "ACTIVE" or any(service[k] != 0 for k in ("desiredCount", "runningCount", "pendingCount")):
        raise RuntimeError("Service must be fully inactive before deploying ingress")
    secret = session.client("secretsmanager").describe_secret(SecretId=tls_secret_arn)
    if secret["Name"] != "/kartoush/demo/tls" or not secret["ARN"].startswith(f"arn:aws:secretsmanager:{REGION}:{PROJECT_ID}:secret:"):
        raise RuntimeError("Unexpected TLS secret")
    tags = {t["Key"]: t["Value"] for t in secret.get("Tags", [])}
    if tags.get("Project") != "kartoush" or tags.get("Environment") != "demo":
        raise RuntimeError("TLS secret must belong to the demo environment")
    session.client("ecr").describe_images(repositoryName="kartoush-demo-proxy", imageIds=[{"imageDigest": "sha256:" + proxy_image[len(prefix):]}])
    logs = session.client("logs")
    try:
        logs.create_log_group(logGroupName="/kartoush/demo/proxy", tags={"Project": "kartoush", "Environment": "demo", "Name": "/kartoush/demo/proxy"})
    except logs.exceptions.ResourceAlreadyExistsException:
        pass
    logs.put_retention_policy(logGroupName="/kartoush/demo/proxy", retentionInDays=7)
    task = json.loads((ROOT / "task-definition.template.json").read_text())
    for container in task["containerDefinitions"]:
        if container["name"] in ("proxy", "tls-init"):
            container["image"] = proxy_image
        if container["name"] == "tls-init":
            for value in container["secrets"]:
                value["valueFrom"] = value["valueFrom"].replace("REPLACE_WITH_TLS_SECRET_ARN", secret["ARN"])
    definition = ecs.register_task_definition(**task)["taskDefinition"]
    updated = ecs.update_service(cluster=CLUSTER, service=SERVICE, taskDefinition=definition["taskDefinitionArn"], desiredCount=0)["service"]
    return {"serviceArn": updated["serviceArn"], "taskDefinitionArn": definition["taskDefinitionArn"], "desiredCount": updated["desiredCount"], "proxyImage": proxy_image, "tlsSecretArn": secret["ARN"]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", default="kartoush")
    parser.add_argument("--proxy-image", required=True)
    parser.add_argument("--tls-secret-arn", required=True)
    args = parser.parse_args()
    try:
        session = boto3.Session(profile_name=args.profile, region_name=REGION)
        print(json.dumps(deploy(session, args.proxy_image, args.tls_secret_arn), indent=2))
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
