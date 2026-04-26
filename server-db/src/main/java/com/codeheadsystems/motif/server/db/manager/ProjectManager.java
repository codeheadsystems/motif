package com.codeheadsystems.motif.server.db.manager;

import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.dao.ProjectDao;
import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Project;
import com.codeheadsystems.motif.server.db.model.Tier;
import com.codeheadsystems.motif.server.db.model.Timestamp;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jdbi.v3.core.Jdbi;

/**
 * Premium-tier manager: every entry point calls {@link Tiers#require(Owner, Tier)}
 * with PREMIUM. A FREE_SYNCED owner cannot reach the projects table — defense in
 * depth on top of the resource-layer check.
 */
@Singleton
public class ProjectManager {

  private final Jdbi jdbi;
  private final ProjectDao projectDao;

  @Inject
  public ProjectManager(final Jdbi jdbi, final ProjectDao projectDao) {
    this.jdbi = jdbi;
    this.projectDao = projectDao;
  }

  public Optional<Project> get(Owner owner, Identifier identifier) {
    Tiers.require(owner, Tier.PREMIUM);
    return projectDao.findByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid());
  }

  public Optional<Project> findByName(Owner owner, String name) {
    Tiers.require(owner, Tier.PREMIUM);
    return projectDao.findByOwnerAndName(owner.identifier().uuid(), name.strip());
  }

  public Page<Project> findByOwner(Owner owner, PageRequest pageRequest) {
    Tiers.require(owner, Tier.PREMIUM);
    List<Project> results = projectDao.findByOwner(
        owner.identifier().uuid(),
        pageRequest.pageSize() + 1,
        pageRequest.offset());
    return Page.of(results, pageRequest);
  }

  public void store(Owner owner, Project project) {
    Tiers.require(owner, Tier.PREMIUM);
    projectDao.upsert(
        project.identifier().uuid(),
        project.ownerIdentifier().uuid(),
        project.name(),
        project.description(),
        project.status().name(),
        project.createdAt().toOffsetDateTime(),
        project.updatedAt().toOffsetDateTime());
  }

  public void update(Owner owner, Project project) {
    Tiers.require(owner, Tier.PREMIUM);
    jdbi.useTransaction(handle -> {
      ProjectDao txDao = handle.attach(ProjectDao.class);
      Optional<Project> existing = txDao.findByOwnerAndIdentifier(
          project.ownerIdentifier().uuid(), project.identifier().uuid());
      if (existing.isEmpty()) {
        throw new NotFoundException("Project not found: " + project.identifier().uuid());
      }
      // Update bumps updated_at; preserve created_at from the existing row.
      Project toWrite = Project.from(project)
          .createdAt(existing.get().createdAt())
          .updatedAt(new Timestamp())
          .build();
      txDao.upsert(
          toWrite.identifier().uuid(),
          toWrite.ownerIdentifier().uuid(),
          toWrite.name(),
          toWrite.description(),
          toWrite.status().name(),
          toWrite.createdAt().toOffsetDateTime(),
          toWrite.updatedAt().toOffsetDateTime());
    });
  }

  public boolean delete(Owner owner, Identifier identifier) {
    Tiers.require(owner, Tier.PREMIUM);
    return projectDao.deleteByOwnerAndIdentifier(owner.identifier().uuid(), identifier.uuid()) > 0;
  }
}
