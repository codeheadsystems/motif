# Motif Infrastructure (AWS CDK, TypeScript)

CDK v2 application for the Motif production environment on AWS (`us-east-1` to start).
Dev work runs against [LocalStack](https://localstack.cloud) via `cdklocal`.

## Stacks

| Stack | Purpose | LocalStack Community? |
|---|---|---|
| `motif-dev-network` | VPC, subnets, security groups | Partial — synth works; deploy may be limited |
| `motif-dev-storage` | S3 attachments bucket + ECR server image repo | **Yes** — fully supported |
| `motif-dev-secrets` | OPAQUE seeds, JWT secret, Aurora credentials in Secrets Manager | **Yes** — fully supported |
| `motif-dev-database` | Aurora PostgreSQL serverless v2 cluster | **No** — Aurora is LocalStack Pro only |
| `motif-dev-application` | ECS Fargate service + ALB | **No** — ECS/Fargate are LocalStack Pro only |

For full local integration testing, the Postgres container in the repo's `docker-compose.yml`
substitutes for Aurora. Application deployment is validated only against real AWS.

## One-time setup

```bash
cd infra
npm install
```

## Synth (validate CloudFormation output — no AWS credentials required)

```bash
npm run synth         # synth default stack (first one)
npm run synth:all     # synth all stacks
```

CloudFormation templates land in `cdk.out/`.

## Deploy to LocalStack

Requires Docker and the LocalStack container running locally
(see `docker-compose.yml` in the repo root — add a `localstack` service per Phase 0 plan):

```bash
docker compose up localstack -d
npm run bootstrap:local            # one-time per LocalStack instance
npm run deploy:local               # deploys network + storage + secrets (Aurora/ECS will error)
```

Stacks with `# Not on LocalStack free` comments (`motif-dev-database`, `motif-dev-application`)
will fail to deploy on Community LocalStack. Either deploy them to real AWS, skip them,
or upgrade to LocalStack Pro.

## Deploy to real AWS

```bash
export CDK_DEFAULT_ACCOUNT=<your-aws-account-id>
export CDK_DEFAULT_REGION=us-east-1
export AWS_PROFILE=<your-profile>

npx cdk bootstrap                  # one-time per account/region
npx cdk deploy --all --context stage=dev
```

After deploy, populate the Secrets Manager entries with real random values:

```bash
for s in opaque/server-key-seed opaque/oprf-seed opaque/oprf-master-key jwt/signing-secret; do
  aws secretsmanager put-secret-value \
    --secret-id "motif-dev-secrets/${s}" \
    --secret-string "$(openssl rand -hex 32)"
done
```

## Project layout

```
infra/
├── bin/motif.ts             # CDK app entry point
├── lib/
│   ├── motif-network-stack.ts
│   ├── motif-storage-stack.ts
│   ├── motif-secrets-stack.ts
│   ├── motif-database-stack.ts
│   └── motif-application-stack.ts
├── cdk.json
├── package.json
└── tsconfig.json
```

## Known Phase 0 limitations

- **No domain / no TLS**: ALB listener is HTTP on port 80. TLS + ACM + Route53 added when a domain is registered.
- **Aurora / ECS not deployable on LocalStack Community**: synth-only until real-AWS account exists.
- **Empty secret values**: Secrets Manager entries are created with no value; populate post-deploy via `aws secretsmanager put-secret-value`.
- **No CI deploy**: Phase 0 ends with `cdk synth` running on PRs. Actual deploy pipeline comes later.
