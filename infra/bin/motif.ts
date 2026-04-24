#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { MotifNetworkStack } from '../lib/motif-network-stack';
import { MotifStorageStack } from '../lib/motif-storage-stack';
import { MotifSecretsStack } from '../lib/motif-secrets-stack';
import { MotifDatabaseStack } from '../lib/motif-database-stack';
import { MotifApplicationStack } from '../lib/motif-application-stack';

const app = new cdk.App();

// Environment:
//   - With CDK_DEFAULT_ACCOUNT set (real AWS): full environment, region-specific lookups allowed.
//   - Without CDK_DEFAULT_ACCOUNT (local synth / LocalStack): "agnostic" stack with region only,
//     which suppresses AWS-API lookups at synth time so credentials are not required.
const region = process.env.CDK_DEFAULT_REGION ?? 'us-east-1';
const env: cdk.Environment = process.env.CDK_DEFAULT_ACCOUNT
  ? { account: process.env.CDK_DEFAULT_ACCOUNT, region }
  : { region };

// Stage name prefix — swap via `cdk --context stage=prod` when deploying to real AWS.
const stage = app.node.tryGetContext('stage') ?? 'dev';
const prefix = `motif-${stage}`;

const network = new MotifNetworkStack(app, `${prefix}-network`, { env });

const storage = new MotifStorageStack(app, `${prefix}-storage`, { env });

const secrets = new MotifSecretsStack(app, `${prefix}-secrets`, { env });

const database = new MotifDatabaseStack(app, `${prefix}-database`, {
  env,
  vpc: network.vpc,
});

new MotifApplicationStack(app, `${prefix}-application`, {
  env,
  vpc: network.vpc,
  attachmentsBucket: storage.attachmentsBucket,
  ecrRepository: storage.ecrRepository,
  opaqueSecrets: secrets.opaqueSecrets,
  jwtSecret: secrets.jwtSecret,
  databaseSecret: database.masterCredentialsSecret,
});

app.synth();
