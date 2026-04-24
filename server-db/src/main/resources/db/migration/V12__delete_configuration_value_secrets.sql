-- Remove OPAQUE and JWT secrets from configuration_values.
-- Secrets now come from environment variables (dev) / AWS Secrets Manager (prod),
-- loaded via Dropwizard EnvironmentVariableSubstitutor at startup.
--
-- The configuration_values table itself is retained for non-secret runtime config.
DELETE FROM configuration_values WHERE key IN (
    'hofmann.serverKeySeedHex',
    'hofmann.oprfSeedHex',
    'hofmann.oprfMasterKeyHex',
    'hofmann.jwtSecretHex',
    'hofmann.context'
);
