package com.codeheadsystems.motif.server.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeheadsystems.motif.server.db.DatabaseTest;
import com.codeheadsystems.motif.server.db.model.OpaqueCredentialRecord;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpaqueCredentialDaoTest extends DatabaseTest {

  private OpaqueCredentialDao dao;

  @BeforeEach
  void setUp() {
    jdbi.useHandle(h -> h.execute("DELETE FROM opaque_credentials"));
    dao = jdbi.onDemand(OpaqueCredentialDao.class);
  }

  @Test
  void upsertAndFind() {
    byte[] credId = {1, 2, 3};
    dao.upsert(credId, new byte[]{10}, new byte[]{20}, new byte[]{30}, new byte[]{40}, 0);

    Optional<OpaqueCredentialRecord> result = dao.findByCredentialId(credId);

    assertThat(result).isPresent();
    assertThat(result.get().clientPublicKey()).isEqualTo(new byte[]{10});
    assertThat(result.get().maskingKey()).isEqualTo(new byte[]{20});
    assertThat(result.get().envelopeNonce()).isEqualTo(new byte[]{30});
    assertThat(result.get().authTag()).isEqualTo(new byte[]{40});
    assertThat(result.get().keyVersion()).isEqualTo(0);
  }

  @Test
  void findReturnsEmptyWhenNotFound() {
    assertThat(dao.findByCredentialId(new byte[]{99})).isEmpty();
  }

  @Test
  void upsertUpdatesExisting() {
    byte[] credId = {1, 2, 3};
    dao.upsert(credId, new byte[]{10}, new byte[]{20}, new byte[]{30}, new byte[]{40}, 0);
    dao.upsert(credId, new byte[]{11}, new byte[]{21}, new byte[]{31}, new byte[]{41}, 1);

    OpaqueCredentialRecord result = dao.findByCredentialId(credId).orElseThrow();
    assertThat(result.clientPublicKey()).isEqualTo(new byte[]{11});
    assertThat(result.keyVersion()).isEqualTo(1);
  }

  @Test
  void deleteRemovesRecord() {
    byte[] credId = {1, 2, 3};
    dao.upsert(credId, new byte[]{10}, new byte[]{20}, new byte[]{30}, new byte[]{40}, 0);

    assertThat(dao.deleteByCredentialId(credId)).isEqualTo(1);
    assertThat(dao.findByCredentialId(credId)).isEmpty();
  }

  @Test
  void deleteReturnsZeroWhenNotFound() {
    assertThat(dao.deleteByCredentialId(new byte[]{99})).isEqualTo(0);
  }
}
