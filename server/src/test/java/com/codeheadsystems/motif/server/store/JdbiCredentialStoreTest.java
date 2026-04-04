package com.codeheadsystems.motif.server.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeheadsystems.hofmann.server.store.VersionedCredential;
import com.codeheadsystems.motif.server.db.dao.OpaqueCredentialDao;
import com.codeheadsystems.motif.server.db.model.OpaqueCredentialRecord;
import com.codeheadsystems.rfc.opaque.model.Envelope;
import com.codeheadsystems.rfc.opaque.model.RegistrationRecord;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JdbiCredentialStoreTest {

  @Mock
  private OpaqueCredentialDao dao;

  private JdbiCredentialStore store;

  private static final byte[] CRED_ID = {1, 2, 3};
  private static final byte[] PUB_KEY = {10, 11};
  private static final byte[] MASK_KEY = {20, 21};
  private static final byte[] NONCE = {30, 31};
  private static final byte[] TAG = {40, 41};

  @BeforeEach
  void setUp() {
    store = new JdbiCredentialStore();
    store.initialize(dao);
  }

  @Test
  void throwsWhenNotInitialized() {
    JdbiCredentialStore uninit = new JdbiCredentialStore();
    assertThatThrownBy(() -> uninit.load(CRED_ID))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void storeDecomposesRegistrationRecord() {
    RegistrationRecord record = new RegistrationRecord(PUB_KEY, MASK_KEY, new Envelope(NONCE, TAG));

    store.store(CRED_ID, record, 5);

    verify(dao).upsert(CRED_ID, PUB_KEY, MASK_KEY, NONCE, TAG, 5);
  }

  @Test
  void loadReconstructsRegistrationRecord() {
    when(dao.findByCredentialId(CRED_ID))
        .thenReturn(Optional.of(new OpaqueCredentialRecord(CRED_ID, PUB_KEY, MASK_KEY, NONCE, TAG, 0)));

    Optional<RegistrationRecord> result = store.load(CRED_ID);

    assertThat(result).isPresent();
    assertThat(result.get().clientPublicKey()).isEqualTo(PUB_KEY);
    assertThat(result.get().maskingKey()).isEqualTo(MASK_KEY);
    assertThat(result.get().envelope().envelopeNonce()).isEqualTo(NONCE);
    assertThat(result.get().envelope().authTag()).isEqualTo(TAG);
  }

  @Test
  void loadReturnsEmptyWhenNotFound() {
    when(dao.findByCredentialId(CRED_ID)).thenReturn(Optional.empty());

    assertThat(store.load(CRED_ID)).isEmpty();
  }

  @Test
  void loadVersionedIncludesKeyVersion() {
    when(dao.findByCredentialId(CRED_ID))
        .thenReturn(Optional.of(new OpaqueCredentialRecord(CRED_ID, PUB_KEY, MASK_KEY, NONCE, TAG, 3)));

    Optional<VersionedCredential> result = store.loadVersioned(CRED_ID);

    assertThat(result).isPresent();
    assertThat(result.get().keyVersion()).isEqualTo(3);
    assertThat(result.get().record().clientPublicKey()).isEqualTo(PUB_KEY);
  }

  @Test
  void deleteCallsDao() {
    store.delete(CRED_ID);

    verify(dao).deleteByCredentialId(CRED_ID);
  }
}
