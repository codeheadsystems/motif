import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import { Construct } from 'constructs';

interface MotifApplicationStackProps extends cdk.StackProps {
  vpc: ec2.IVpc;
  attachmentsBucket: s3.IBucket;
  ecrRepository: ecr.IRepository;
  opaqueSecrets: {
    serverKeySeed: secretsmanager.ISecret;
    oprfSeed: secretsmanager.ISecret;
    oprfMasterKey: secretsmanager.ISecret;
  };
  jwtSecret: secretsmanager.ISecret;
  databaseSecret: secretsmanager.ISecret;
}

/**
 * ECS Fargate service + ALB for the Motif Dropwizard server.
 *
 * LocalStack Community: ECS/Fargate NOT supported (Pro feature). `cdk synth` produces
 * valid CloudFormation but `cdklocal deploy` will fail. This stack is validated only
 * against real AWS.
 */
export class MotifApplicationStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: MotifApplicationStackProps) {
    super(scope, id, props);

    const cluster = new ecs.Cluster(this, 'Cluster', {
      vpc: props.vpc,
      containerInsightsV2: ecs.ContainerInsights.ENABLED,
    });

    const logGroup = new logs.LogGroup(this, 'ServerLogs', {
      retention: logs.RetentionDays.ONE_MONTH,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    const taskDefinition = new ecs.FargateTaskDefinition(this, 'ServerTask', {
      cpu: 512,
      memoryLimitMiB: 1024,
    });

    const container = taskDefinition.addContainer('server', {
      image: ecs.ContainerImage.fromEcrRepository(props.ecrRepository, 'latest'),
      logging: ecs.LogDrivers.awsLogs({ logGroup, streamPrefix: 'motif' }),
      environment: {
        // Dropwizard reads YAML at /app/config.yml which references these env vars.
        // Non-secret configuration lives in config.yml.
      },
      secrets: {
        MOTIF_OPAQUE_SERVER_KEY_SEED_HEX: ecs.Secret.fromSecretsManager(props.opaqueSecrets.serverKeySeed),
        MOTIF_OPAQUE_OPRF_SEED_HEX: ecs.Secret.fromSecretsManager(props.opaqueSecrets.oprfSeed),
        MOTIF_OPAQUE_OPRF_MASTER_KEY_HEX: ecs.Secret.fromSecretsManager(props.opaqueSecrets.oprfMasterKey),
        MOTIF_JWT_SECRET_HEX: ecs.Secret.fromSecretsManager(props.jwtSecret),
      },
      portMappings: [{ containerPort: 8080 }],
    });

    // Grant the task read access to the attachments bucket and DB secret.
    props.attachmentsBucket.grantReadWrite(taskDefinition.taskRole);
    props.databaseSecret.grantRead(taskDefinition.taskRole);

    const service = new ecs.FargateService(this, 'Service', {
      cluster,
      taskDefinition,
      desiredCount: 2,
      assignPublicIp: false,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      enableExecuteCommand: false,
      minHealthyPercent: 100,
      maxHealthyPercent: 200,
    });

    // TODO(phase-1): wire Fargate → Aurora connectivity via a shared dbAccess SG created
    // in the database stack and attached to this service. Synth-only in Phase 0 — actual
    // deploy will require this rule before the server can reach the database.

    const alb = new elbv2.ApplicationLoadBalancer(this, 'Alb', {
      vpc: props.vpc,
      internetFacing: true,
    });

    const listener = alb.addListener('Listener', {
      port: 80,
      // TLS termination: listener will be replaced with HTTPS + ACM cert once a domain is registered.
    });

    listener.addTargets('ServerTarget', {
      port: 8080,
      targets: [service],
      healthCheck: {
        path: '/',
        healthyHttpCodes: '200-299',
      },
    });

    new cdk.CfnOutput(this, 'AlbDnsName', {
      value: alb.loadBalancerDnsName,
      description: 'Application Load Balancer DNS name',
    });
  }
}
