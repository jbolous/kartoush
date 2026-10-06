#!/usr/bin/env python3
"""Create the demo app secret once; never print its values or replace its key."""

import argparse
import base64
import json
import secrets
import sys

import boto3
from botocore.exceptions import BotoCoreError, ClientError

REGION = "us-east-2"
PROJECT_ID = "790873128308"
SECRET_NAME = "/kartoush/demo/app"


def create_app_secret(session):
    identity = session.client("sts").get_caller_identity()
    if identity["Account"] != PROJECT_ID:
        raise RuntimeError("Wrong AWS project; refusing to create the secret")

    client = session.client("secretsmanager")
    try:
        client.describe_secret(SecretId=SECRET_NAME)
    except client.exceptions.ResourceNotFoundException:
        pass
    else:
        raise RuntimeError("Secret already exists; preserve its credentials and encryption key")

    values = {
        "db_username": "kartoush_app",
        "db_password": secrets.token_urlsafe(48),
        "internal_admin_username": "kartoush-admin",
        "internal_admin_password": secrets.token_urlsafe(48),
        "activation_email_encryption_key": base64.b64encode(secrets.token_bytes(32)).decode("ascii"),
    }
    response = client.create_secret(
        Name=SECRET_NAME,
        Description="Demo application database/admin credentials and stable job encryption key",
        SecretString=json.dumps(values),
        Tags=[
            {"Key": "Project", "Value": "kartoush"},
            {"Key": "Environment", "Value": "demo"},
            {"Key": "Name", "Value": SECRET_NAME},
        ],
    )
    return {key: response[key] for key in ("ARN", "Name", "VersionId")}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", default="kartoush")
    args = parser.parse_args()
    try:
        session = boto3.Session(profile_name=args.profile, region_name=REGION)
        print(json.dumps(create_app_secret(session), indent=2))
        return 0
    except ClientError as error:
        # Error messages and debug traces can contain request payloads.
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
