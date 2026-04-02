package com.codeheadsystems.motif.server.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeheadsystems.motif.server.dao.OwnerDao;
import com.codeheadsystems.motif.server.model.Identifier;
import com.codeheadsystems.motif.server.model.Owner;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OwnerManagerTest {

  private static final Owner OWNER = new Owner("TEST-OWNER");

  @Mock
  private OwnerDao ownerDao;

  private OwnerManager ownerManager;

  @BeforeEach
  void setUp() {
    ownerManager = new OwnerManager(ownerDao);
  }

  // --- store ---

  @Test
  void storeCallsUpsertWithCorrectArgs() {
    ownerManager.store(OWNER);

    verify(ownerDao).upsert(OWNER.identifier().uuid(), OWNER.value());
  }

  // --- get ---

  @Test
  void getDelegatesToDao() {
    when(ownerDao.findByIdentifier(OWNER.identifier().uuid()))
        .thenReturn(Optional.of(OWNER));

    Optional<Owner> result = ownerManager.get(OWNER.identifier());

    assertThat(result).contains(OWNER);
  }

  @Test
  void getReturnsEmptyWhenNotFound() {
    Identifier unknown = new Identifier();
    when(ownerDao.findByIdentifier(unknown.uuid())).thenReturn(Optional.empty());

    assertThat(ownerManager.get(unknown)).isEmpty();
  }

  // --- delete ---

  @Test
  void deleteReturnsTrueWhenRowDeleted() {
    when(ownerDao.deleteByIdentifier(OWNER.identifier().uuid())).thenReturn(1);

    assertThat(ownerManager.delete(OWNER.identifier())).isTrue();
  }

  @Test
  void deleteReturnsFalseWhenNoRowDeleted() {
    when(ownerDao.deleteByIdentifier(OWNER.identifier().uuid())).thenReturn(0);

    assertThat(ownerManager.delete(OWNER.identifier())).isFalse();
  }

  // --- find ---

  @Test
  void findDelegatesToDaoWithUpperCasedValue() {
    when(ownerDao.findByValue("FINDABLE")).thenReturn(Optional.of(OWNER));

    Optional<Owner> result = ownerManager.find("findable");

    assertThat(result).contains(OWNER);
  }

  @Test
  void findReturnsEmptyWhenNotFound() {
    when(ownerDao.findByValue("NONEXISTENT")).thenReturn(Optional.empty());

    assertThat(ownerManager.find("nonexistent")).isEmpty();
  }
}
