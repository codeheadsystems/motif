package com.codeheadsystems.motif.server.store;

import com.codeheadsystems.hofmann.server.store.CredentialStore;
import com.codeheadsystems.hofmann.server.store.VersionedCredential;
import com.codeheadsystems.motif.server.db.dao.OpaqueCredentialDao;
import com.codeheadsystems.rfc.opaque.model.Envelope;
import com.codeheadsystems.rfc.opaque.model.RegistrationRecord;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PostgreSQL-backed {@link CredentialStore} using JDBI.
 * Must be initialized with {@link #initialize(OpaqueCredentialDao)} before use.
 */
public class JdbiCredentialStore implements CredentialStore {

  private final AtomicReference<OpaqueCredentialDao> daoRef = new AtomicReference<>();

  public void initialize(OpaqueCredentialDao dao) {
    if (!daoRef.compareAndSet(null, dao)) {
      throw new IllegalStateException("JdbiCredentialStore already initialized");
    }
  }

  @Override
  public void store(byte[] credentialIdentifier, RegistrationRecord record) {
    store(credentialIdentifier, record, 0);
  }

  @Override
  public void store(byte[] credentialIdentifier, RegistrationRecord record, int keyVersion) {
    dao().upsert(
        credentialIdentifier,
        record.clientPublicKey(),
        record.maskingKey(),
        record.envelope().envelopeNonce(),
        record.envelope().authTag(),
        keyVersion);
  }

  @Override
  public Optional<RegistrationRecord> load(byte[] credentialIdentifier) {
    return dao().findByCredentialId(credentialIdentifier)
        .map(r -> new RegistrationRecord(
            r.clientPublicKey(),
            r.maskingKey(),
            new Envelope(r.envelopeNonce(), r.authTag())));
  }

  @Override
  public Optional<VersionedCredential> loadVersioned(byte[] credentialIdentifier) {
    return dao().findByCredentialId(credentialIdentifier)
        .map(r -> new VersionedCredential(
            r.keyVersion(),
            new RegistrationRecord(
                r.clientPublicKey(),
                r.maskingKey(),
                new Envelope(r.envelopeNonce(), r.authTag()))));
  }

  @Override
  public void delete(byte[] credentialIdentifier) {
    dao().deleteByCredentialId(credentialIdentifier);
  }

  private OpaqueCredentialDao dao() {
    OpaqueCredentialDao dao = daoRef.get();
    if (dao == null) {
      throw new IllegalStateException("JdbiCredentialStore not initialized");
    }
    return dao;
  }
}
