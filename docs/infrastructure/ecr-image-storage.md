# Demo Image Storage

Private ECR repositories `kartoush-demo-app` and `kartoush-demo-proxy` live in Ohio (`us-east-2`) in project `790873128308`. They match the existing execution-role pull permissions. Both use AES256 encryption and immutable tags. Runtime deployment uses image digests, not floating tags. This work stores images; it does not start ECS or provision RDS.

The project is on the active Free plan. The local `kartoush` profile selects Ohio; confirm the project's selected Region in AWS Settings > View all projects > Overview > Additional Info > Region before recreating resources.

## Repository Configuration

For a new environment, create each repository with temporary operator credentials:

```bash
for repository in kartoush-demo-app kartoush-demo-proxy; do
  aws ecr create-repository --profile kartoush --region us-east-2 \
    --repository-name "$repository" --image-tag-mutability IMMUTABLE \
    --encryption-configuration encryptionType=AES256 \
    --tags Key=Project,Value=kartoush Key=Environment,Value=demo
done
```

Existing repositories should be inspected rather than recreated. Registry-level basic scanning is scoped to `kartoush-demo-*`. The configuration replaces registry scanning rules: inspect existing rules and preserve unrelated repositories before applying it elsewhere. Enhanced scanning is not enabled.

```bash
aws ecr get-registry-scanning-configuration --profile kartoush --region us-east-2
aws ecr put-registry-scanning-configuration --profile kartoush --region us-east-2 \
  --cli-input-json file://infrastructure/aws/ecr/scanning-configuration.json
```

## Retention and Rollback

The policy expires untagged images after seven days and retains the three newest `sha-` releases in each repository. Use only `sha-` tags for published releases. Before applying the policy, run `start-lifecycle-policy-preview`, wait for `get-lifecycle-policy-preview` to return `COMPLETE`, and inspect every expiration. Apply only when the current deployment and previous working digest are retained.

```bash
for repository in kartoush-demo-app kartoush-demo-proxy; do
  aws ecr start-lifecycle-policy-preview --profile kartoush --region us-east-2 \
    --repository-name "$repository" \
    --lifecycle-policy-text file://infrastructure/aws/ecr/lifecycle-policy.json
  aws ecr get-lifecycle-policy-preview --profile kartoush --region us-east-2 \
    --repository-name "$repository"
done
```

After inspecting completed previews:

```bash
for repository in kartoush-demo-app kartoush-demo-proxy; do
  aws ecr put-lifecycle-policy --profile kartoush --region us-east-2 \
    --repository-name "$repository" \
    --lifecycle-policy-text file://infrastructure/aws/ecr/lifecycle-policy.json
done
```

A count policy does not understand deployed or working images. Before publishing a fourth distinct image, confirm the active and rollback digests remain among the newest three after publication. Stop publication if either would age out; adjust retention and cost estimates deliberately before proceeding. Do not fill the release repository with speculative builds. Review aggregate retained storage against the architecture's 2 GiB allowance; image count is not a byte limit. Images may be expired within 24 hours after becoming eligible. See [AWS lifecycle policy guidance](https://docs.aws.amazon.com/AmazonECR/latest/userguide/LifecyclePolicies.html).

## Application Publication

Build from a clean checkout of the selected committed application source. Linux/amd64 matches the planned Linux/x86 Fargate task; an Apple Silicon default image does not. Set the immutable tag from that checkout's full commit SHA:

```bash
release_tag="sha-$(git rev-parse HEAD)"
registry=790873128308.dkr.ecr.us-east-2.amazonaws.com
image_uri="$registry/kartoush-demo-app:$release_tag"
docker buildx build --platform linux/amd64 --provenance=false --load \
  --tag "$image_uri" .
```

Provenance is disabled for this single-platform manual release to avoid auxiliary untagged attestation manifests; the recorded commit and ECR digest identify this artifact. Rebuilding the same source can produce a different digest when upstream dependencies change. Use a new explicit release tag for a rebuild; immutable tags must not be overwritten.

Authenticate through stdin, then push and inspect:

```bash
aws ecr get-login-password --profile kartoush --region us-east-2 \
  | docker login --username AWS --password-stdin "$registry"
docker push "$image_uri"
aws ecr describe-images --profile kartoush --region us-east-2 \
  --repository-name kartoush-demo-app --image-ids imageTag="$release_tag"
docker logout "$registry"
```

The login token lasts 12 hours. Never print it or commit Docker credentials. Basic scanning is asynchronous; inspect findings with `describe-image-scan-findings` before deployment acceptance. Basic scanning covers OS packages and does not establish that the application is vulnerability-free. See [AWS basic scanning guidance](https://docs.aws.amazon.com/AmazonECR/latest/userguide/image-scanning-basic.html).

## Proxy Handoff

The proxy repository is ready but empty. #341 must supply the approved Linux/amd64 proxy and certificate-initialization image, required `script`/`cat` utilities for any Exec target, and route/TLS configuration. Publish that tested image under an immutable `sha-` tag and record its ECR digest before accepting #176 as fully aligned with the architecture. An upstream proxy image alone does not establish the required ingress behavior. #177 must reference both published application and proxy digests.

## Published Application Evidence

On October 5, 2026 (local time), the merged #388 source commit `4ff92929aa039ffd2d4ac780fe7bf9a8cbe4c8d1` was built as Linux/amd64 and pushed under its full-SHA tag. The published digest is recorded in [application-release.json](../../infrastructure/aws/ecr/application-release.json). Use this reference in the later task definition:

```text
790873128308.dkr.ecr.us-east-2.amazonaws.com/kartoush-demo-app@sha256:f29b648df23208ee53b6d0f6f867307d3bd710f5701d5579f5dad45e4aebd756
```

ECR reported 177,650,747 bytes (about 169 MiB compressed), and `batch-get-image` retrieved the manifest by digest without failures. Runtime checks under local x86 emulation passed for Java 25, UID 10001, the loopback address default, certificate-bundle permissions, and `script`/`cat`. Full native x86 startup/resource-budget and execution-role pull validation belong to task acceptance. No local Java unit or integration suite was rerun for the storage-only changes.

Both empty-repository lifecycle previews completed with zero expirations before the policies were applied. Registry scanning was initially BASIC with no rules, so the scoped scan-on-push configuration replaced no existing filters. Temporary Docker login credentials were logged out and removed. The repositories and published image are currently retained for deployment; no compute was started. Basic scanning completed successfully with no reported OS findings for this digest; this does not cover all Java dependency or application vulnerabilities.
