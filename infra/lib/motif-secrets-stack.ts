import * as cdk from 'aws-cdk-lib';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import { Construct } from 'constructs';

/**
 * AWS Secrets Manager entries for OPAQUE seeds, OPRF master key, JWT signing secret,
 * and the Aurora cluster's master credentials.
 *
 * Values are created empty (placeholder) — real secrets are written post-deployment via:
 *   aws secretsmanager put-secret-value --secret-id <name> --secret-string $(openssl rand -hex 32)
 *
 * LocalStack Community: Secrets Manager fully supported. Safe to `cdklocal deploy`.
 */
export class MotifSecretsStack extends cdk.Stack {
  public readonly opaqueSecrets: {
    serverKeySeed: secretsmanager.Secret;
    oprfSeed: secretsmanager.Secret;
    oprfMasterKey: secretsmanager.Secret;
  };
  public readonly jwtSecret: secretsmanager.Secret;

  constructor(scope: Construct, id: string, props: cdk.StackProps) {
    super(scope, id, props);

    // OPAQUE and JWT secrets: plain-string values, filled in post-deploy with `openssl rand -hex 32`.
    // Each is a 32-byte hex-encoded string (64 characters).
    this.opaqueSecrets = {
      serverKeySeed: new secretsmanager.Secret(this, 'OpaqueServerKeySeed', {
        secretName: `${id}/opaque/server-key-seed`,
        description: 'Hex-encoded 32-byte OPAQUE server AKE key seed',
      }),
      oprfSeed: new secretsmanager.Secret(this, 'OpaqueOprfSeed', {
        secretName: `${id}/opaque/oprf-seed`,
        description: 'Hex-encoded 32-byte OPRF seed',
      }),
      oprfMasterKey: new secretsmanager.Secret(this, 'OpaqueOprfMasterKey', {
        secretName: `${id}/opaque/oprf-master-key`,
        description: 'Hex-encoded 32-byte OPRF master key (non-zero P-256 scalar)',
      }),
    };

    this.jwtSecret = new secretsmanager.Secret(this, 'JwtSigningSecret', {
      secretName: `${id}/jwt/signing-secret`,
      description: 'Hex-encoded 32-byte JWT HMAC-SHA256 signing secret',
    });
    // Aurora master credentials are created inside MotifDatabaseStack to avoid a
    // cross-stack dependency cycle caused by rds.Credentials.fromSecret().
  }
}
