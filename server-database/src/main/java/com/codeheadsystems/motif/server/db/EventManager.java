package com.codeheadsystems.motif.server.db;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EventManager {

  private final EventDao eventDao;
  private final TagsDao tagsDao;

  @Inject
  public EventManager(final EventDao eventDao, final TagsDao tagsDao) {
    this.eventDao = eventDao;
    this.tagsDao = tagsDao;
  }
}
