package com.codeheadsystems.motif.server.manager;

import com.codeheadsystems.motif.server.dao.EventDao;
import com.codeheadsystems.motif.server.dao.TagsDao;
import com.codeheadsystems.motif.server.model.Event;
import com.codeheadsystems.motif.server.model.Subject;
import com.codeheadsystems.motif.server.model.Timestamp;
import java.util.Collections;
import java.util.List;
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

  public boolean store(Event event) {
    return false;
  }

  public boolean delete(Event event) {
    return false;
  }

  public boolean update(Event event) {
    return false;
  }

  public boolean create(Event event) {
    return false;
  }

  public List<Event> get(Subject subject, Timestamp start, Timestamp end) {
    return Collections.emptyList();
  }


}
