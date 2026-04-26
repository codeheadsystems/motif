package com.codeheadsystems.motif.server.db.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.DatabaseTest;
import com.codeheadsystems.motif.server.db.dao.OwnerDao;
import com.codeheadsystems.motif.server.db.dao.ProjectDao;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Project;
import com.codeheadsystems.motif.server.db.model.ProjectStatus;
import com.codeheadsystems.motif.server.db.model.Tier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectManagerIntegrationTest extends DatabaseTest {

  private OwnerDao ownerDao;
  private ProjectManager projectManager;

  @BeforeEach
  void setUp() {
    jdbi.useHandle(handle -> {
      handle.execute("DELETE FROM projects");
      handle.execute("DELETE FROM owners");
    });
    ownerDao = jdbi.onDemand(OwnerDao.class);
    projectManager = new ProjectManager(jdbi, jdbi.onDemand(ProjectDao.class));
  }

  private Owner createOwner(String value, Tier tier) {
    Owner owner = Owner.builder().value(value).tier(tier).build();
    ownerDao.upsert(owner.identifier().uuid(), owner.value(), false, tier.name());
    return owner;
  }

  @Test
  void freeSyncedOwnerCannotCreateProject() {
    Owner free = createOwner("FREE-USER", Tier.FREE_SYNCED);
    Project project = Project.builder().owner(free).name("X").status(ProjectStatus.ACTIVE).build();

    assertThatThrownBy(() -> projectManager.store(free, project))
        .isInstanceOf(TierRequiredException.class)
        .satisfies(e -> {
          TierRequiredException tre = (TierRequiredException) e;
          assertThat(tre.required()).isEqualTo(Tier.PREMIUM);
          assertThat(tre.actual()).isEqualTo(Tier.FREE_SYNCED);
        });
  }

  @Test
  void freeSyncedOwnerCannotListProjects() {
    Owner free = createOwner("FREE-USER", Tier.FREE_SYNCED);
    assertThatThrownBy(() -> projectManager.findByOwner(free, PageRequest.first()))
        .isInstanceOf(TierRequiredException.class);
  }

  @Test
  void premiumOwnerCanStoreAndFetch() {
    Owner premium = createOwner("PREMIUM-USER", Tier.PREMIUM);
    Project project = Project.builder().owner(premium).name("Garden").status(ProjectStatus.ACTIVE).build();

    projectManager.store(premium, project);

    var fetched = projectManager.get(premium, project.identifier());
    assertThat(fetched).isPresent();
    assertThat(fetched.get().name()).isEqualTo("Garden");
  }

  @Test
  void businessOwnerAlsoSatisfiesPremiumGate() {
    Owner biz = createOwner("BIZ-USER", Tier.BUSINESS);
    Project project = Project.builder().owner(biz).name("Q3 Push").status(ProjectStatus.ACTIVE).build();

    projectManager.store(biz, project);

    assertThat(projectManager.get(biz, project.identifier())).isPresent();
  }

  @Test
  void updateBumpsUpdatedAtAndPreservesCreatedAt() throws InterruptedException {
    Owner premium = createOwner("PREMIUM-USER", Tier.PREMIUM);
    Project original = Project.builder().owner(premium).name("Plant Beds").status(ProjectStatus.ACTIVE).build();
    projectManager.store(premium, original);

    Thread.sleep(10);  // ensure updatedAt is observably later

    Project updated = Project.from(original).status(ProjectStatus.PAUSED).build();
    projectManager.update(premium, updated);

    Project fetched = projectManager.get(premium, original.identifier()).orElseThrow();
    assertThat(fetched.status()).isEqualTo(ProjectStatus.PAUSED);
    // Postgres TIMESTAMPTZ truncates to microseconds; compare epoch millis to ignore nanos.
    assertThat(fetched.createdAt().timestamp().toEpochMilli())
        .isEqualTo(original.createdAt().timestamp().toEpochMilli());
    assertThat(fetched.updatedAt().timestamp().toEpochMilli())
        .isGreaterThanOrEqualTo(original.updatedAt().timestamp().toEpochMilli());
  }

  @Test
  void deleteRemovesRowOnlyForCorrectOwner() {
    Owner alice = createOwner("ALICE", Tier.PREMIUM);
    Owner bob = createOwner("BOB", Tier.PREMIUM);
    Project aliceProject = Project.builder().owner(alice).name("Alice").status(ProjectStatus.ACTIVE).build();
    projectManager.store(alice, aliceProject);

    // Bob trying to delete Alice's project gets a "no such row" — silently no-op, false return.
    assertThat(projectManager.delete(bob, aliceProject.identifier())).isFalse();
    assertThat(projectManager.get(alice, aliceProject.identifier())).isPresent();

    // Alice can delete it.
    assertThat(projectManager.delete(alice, aliceProject.identifier())).isTrue();
    assertThat(projectManager.get(alice, aliceProject.identifier())).isEmpty();
  }

  @Test
  void cascadesOnOwnerDelete() {
    Owner premium = createOwner("PREMIUM-USER", Tier.PREMIUM);
    Project project = Project.builder().owner(premium).name("X").status(ProjectStatus.ACTIVE).build();
    projectManager.store(premium, project);

    // Hard-delete the owner row directly to exercise ON DELETE CASCADE on projects.
    jdbi.useHandle(h -> h.execute("DELETE FROM owners WHERE uuid = ?", premium.identifier().uuid()));

    Identifier id = project.identifier();
    Owner gone = Owner.builder().value("PREMIUM-USER").tier(Tier.PREMIUM)
        .identifier(premium.identifier()).build();
    // Re-insert to bypass tier gate; project should be gone via cascade.
    ownerDao.upsert(gone.identifier().uuid(), gone.value(), false, gone.tier().name());
    assertThat(projectManager.get(gone, id)).isEmpty();
  }
}
