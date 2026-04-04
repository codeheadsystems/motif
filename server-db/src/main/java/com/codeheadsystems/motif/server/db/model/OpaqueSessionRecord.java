package com.codeheadsystems.motif.server.db.model;

import java.time.OffsetDateTime;

public record OpaqueSessionRecord(
    String jti,
    String credentialIdentifier,
    String sessionKey,
    OffsetDateTime issuedAt,
    OffsetDateTime expiresAt) {
}
