package com.codeheadsystems.motif.server.dagger;

import com.codeheadsystems.motif.server.MotifConfiguration;
import com.codeheadsystems.motif.server.db.dao.CategoryDao;
import com.codeheadsystems.motif.server.db.dao.EventDao;
import com.codeheadsystems.motif.server.db.dao.NoteDao;
import com.codeheadsystems.motif.server.db.dao.OwnerDao;
import com.codeheadsystems.motif.server.db.dao.PatternDao;
import com.codeheadsystems.motif.server.db.dao.ProjectDao;
import com.codeheadsystems.motif.server.db.dao.SubjectDao;
import com.codeheadsystems.motif.server.db.dao.TagsDao;
import com.codeheadsystems.motif.server.db.dao.WorkflowDao;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import org.jdbi.v3.core.Jdbi;

@Module
public class MotifModule {

  private final MotifConfiguration configuration;
  private final Jdbi jdbi;

  public MotifModule(final MotifConfiguration configuration, final Jdbi jdbi) {
    this.configuration = configuration;
    this.jdbi = jdbi;
  }

  @Provides
  @Singleton
  MotifConfiguration configuration() {
    return configuration;
  }

  @Provides
  @Singleton
  Jdbi jdbi() {
    return jdbi;
  }

  @Provides
  @Singleton
  OwnerDao ownerDao() {
    return jdbi.onDemand(OwnerDao.class);
  }

  @Provides
  @Singleton
  SubjectDao subjectDao() {
    return jdbi.onDemand(SubjectDao.class);
  }

  @Provides
  @Singleton
  CategoryDao categoryDao() {
    return jdbi.onDemand(CategoryDao.class);
  }

  @Provides
  @Singleton
  EventDao eventDao() {
    return jdbi.onDemand(EventDao.class);
  }

  @Provides
  @Singleton
  NoteDao noteDao() {
    return jdbi.onDemand(NoteDao.class);
  }

  @Provides
  @Singleton
  TagsDao tagsDao() {
    return jdbi.onDemand(TagsDao.class);
  }

  @Provides
  @Singleton
  PatternDao patternDao() {
    return jdbi.onDemand(PatternDao.class);
  }

  @Provides
  @Singleton
  ProjectDao projectDao() {
    return jdbi.onDemand(ProjectDao.class);
  }

  @Provides
  @Singleton
  WorkflowDao workflowDao() {
    return jdbi.onDemand(WorkflowDao.class);
  }
}
