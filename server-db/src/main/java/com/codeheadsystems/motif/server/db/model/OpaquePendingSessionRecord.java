package com.codeheadsystems.motif.server.db.model;

import java.time.OffsetDateTime;

public record OpaquePendingSessionRecord(
    String sessionToken,
    byte[] expectedClientMac,
    byte[] sessionKey,
    String credentialIdentifierB64,
    int keyVersion,
    OffsetDateTime createdAt) {
}
