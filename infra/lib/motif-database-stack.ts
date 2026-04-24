import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import { Construct } from 'constructs';

interface MotifDatabaseStackProps extends cdk.StackProps {
  vpc: ec2.IVpc;
}

/**
 * Aurora PostgreSQL serverless v2 cluster for the Motif application.
 *
 * Owns its own master-credentials secret to avoid cross-stack dependency cycles that
 * arise when a separate secrets stack feeds rds.Credentials.fromSecret().
 *
 * LocalStack Community: Aurora NOT supported (Pro feature). `cdk synth` produces valid
 * CloudFormation but `cdklocal deploy` will fail. Validated only against real AWS. For
 * local development, use the Postgres container from docker-compose.yml.
 */
export class MotifDatabaseStack extends cdk.Stack {
  public readonly cluster: rds.DatabaseCluster;
  public readonly masterCredentialsSecret: secretsmanager.ISecret;

  constructor(scope: Construct, id: string, props: MotifDatabaseStackProps) {
    super(scope, id, props);

    const credentials = rds.Credentials.fromGeneratedSecret('motif_master', {
      secretName: `${id}/master-credentials`,
    });

    this.cluster = new rds.DatabaseCluster(this, 'AuroraCluster', {
      engine: rds.DatabaseClusterEngine.auroraPostgres({
        version: rds.AuroraPostgresEngineVersion.VER_15_8,
      }),
      credentials,
      defaultDatabaseName: 'motif',
      vpc: props.vpc,
      vpcSubnets: {
        subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
      },
      writer: rds.ClusterInstance.serverlessV2('Writer', {
        publiclyAccessible: false,
      }),
      readers: [
        rds.ClusterInstance.serverlessV2('Reader1', {
          publiclyAccessible: false,
          scaleWithWriter: true,
        }),
      ],
      serverlessV2MinCapacity: 0.5,
      serverlessV2MaxCapacity: 4,
      backup: {
        retention: cdk.Duration.days(35),
        preferredWindow: '03:00-04:00',
      },
      storageEncrypted: true,
      iamAuthentication: true,
      removalPolicy: cdk.RemovalPolicy.RETAIN,
    });

    // CDK attaches the generated secret to the cluster; expose it for the application stack.
    this.masterCredentialsSecret = this.cluster.secret!;
  }
}
