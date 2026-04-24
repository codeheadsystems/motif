import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { MotifNetworkStack } from '../lib/motif-network-stack';
import { MotifStorageStack } from '../lib/motif-storage-stack';
import { MotifSecretsStack } from '../lib/motif-secrets-stack';
import { MotifDatabaseStack } from '../lib/motif-database-stack';
import { MotifApplicationStack } from '../lib/motif-application-stack';

const env: cdk.Environment = { region: 'us-east-1' };

describe('Motif CDK stacks', () => {
  test('storage stack provisions a versioned encrypted S3 bucket and an ECR repo', () => {
    const app = new cdk.App();
    const stack = new MotifStorageStack(app, 'test-storage', { env });
    const template = Template.fromStack(stack);
    template.resourceCountIs('AWS::S3::Bucket', 1);
    template.resourceCountIs('AWS::ECR::Repository', 1);
    template.hasResourceProperties('AWS::S3::Bucket', {
      VersioningConfiguration: { Status: 'Enabled' },
    });
  });

  test('secrets stack creates four Secrets Manager entries (3 OPAQUE + 1 JWT)', () => {
    const app = new cdk.App();
    const stack = new MotifSecretsStack(app, 'test-secrets', { env });
    const template = Template.fromStack(stack);
    template.resourceCountIs('AWS::SecretsManager::Secret', 4);
  });

  test('database stack provisions an Aurora Postgres cluster with a serverless v2 writer', () => {
    const app = new cdk.App();
    const network = new MotifNetworkStack(app, 'test-network', { env });
    const stack = new MotifDatabaseStack(app, 'test-database', { env, vpc: network.vpc });
    const template = Template.fromStack(stack);
    template.resourceCountIs('AWS::RDS::DBCluster', 1);
    template.hasResourceProperties('AWS::RDS::DBCluster', {
      Engine: 'aurora-postgresql',
    });
  });

  test('application stack wires secrets into the ECS task definition', () => {
    const app = new cdk.App();
    const network = new MotifNetworkStack(app, 'test-network', { env });
    const storage = new MotifStorageStack(app, 'test-storage', { env });
    const secrets = new MotifSecretsStack(app, 'test-secrets', { env });
    const database = new MotifDatabaseStack(app, 'test-database', { env, vpc: network.vpc });
    const stack = new MotifApplicationStack(app, 'test-app', {
      env,
      vpc: network.vpc,
      attachmentsBucket: storage.attachmentsBucket,
      ecrRepository: storage.ecrRepository,
      opaqueSecrets: secrets.opaqueSecrets,
      jwtSecret: secrets.jwtSecret,
      databaseSecret: database.masterCredentialsSecret,
    });
    const template = Template.fromStack(stack);
    template.resourceCountIs('AWS::ECS::Cluster', 1);
    template.resourceCountIs('AWS::ECS::TaskDefinition', 1);
    template.resourceCountIs('AWS::ElasticLoadBalancingV2::LoadBalancer', 1);
  });
});
