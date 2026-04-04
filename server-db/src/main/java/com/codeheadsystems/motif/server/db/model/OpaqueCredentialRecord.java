package com.codeheadsystems.motif.server.db.model;

public record OpaqueCredentialRecord(
    byte[] credentialId,
    byte[] clientPublicKey,
    byte[] maskingKey,
    byte[] envelopeNonce,
    byte[] authTag,
    int keyVersion) {
}
