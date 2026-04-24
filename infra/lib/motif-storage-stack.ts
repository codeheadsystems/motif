import * as cdk from 'aws-cdk-lib';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import { Construct } from 'constructs';

/**
 * S3 bucket for Motif user attachments and ECR repository for the application image.
 *
 * LocalStack Community: S3 fully supported; ECR supported for basic image push/pull.
 * Safe to `cdklocal deploy`.
 */
export class MotifStorageStack extends cdk.Stack {
  public readonly attachmentsBucket: s3.Bucket;
  public readonly ecrRepository: ecr.Repository;

  constructor(scope: Construct, id: string, props: cdk.StackProps) {
    super(scope, id, props);

    this.attachmentsBucket = new s3.Bucket(this, 'Attachments', {
      versioned: true,
      encryption: s3.BucketEncryption.S3_MANAGED,
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      enforceSSL: true,
      // Keep attachments on stack deletion — user data must not be destroyed by a `cdk destroy`.
      removalPolicy: cdk.RemovalPolicy.RETAIN,
      lifecycleRules: [
        {
          id: 'transition-cold-attachments',
          enabled: true,
          transitions: [
            {
              storageClass: s3.StorageClass.INFREQUENT_ACCESS,
              transitionAfter: cdk.Duration.days(90),
            },
          ],
        },
      ],
    });

    this.ecrRepository = new ecr.Repository(this, 'ServerImage', {
      repositoryName: `${id}-server`.toLowerCase(),
      imageScanOnPush: true,
      // Keep the last N images on deploy; older untagged images age out.
      lifecycleRules: [
        {
          description: 'Retain the 20 most recent images',
          maxImageCount: 20,
        },
      ],
    });
  }
}
