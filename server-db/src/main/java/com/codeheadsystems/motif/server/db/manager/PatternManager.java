package com.codeheadsystems.motif.server.db.manager;

import com.codeheadsystems.motif.common.Page;
import com.codeheadsystems.motif.common.PageRequest;
import com.codeheadsystems.motif.server.db.dao.PatternDao;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Pattern;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Read-side accessor for Patterns. Writes flow through {@link PatternDetectionManager},
 * which owns the replace-on-rerun semantics.
 */
@Singleton
public class PatternManager {

  private final PatternDao patternDao;

  @Inject
  public PatternManager(final PatternDao patternDao) {
    this.patternDao = patternDao;
  }

  public Page<Pattern> findByOwner(Owner owner, PageRequest pageRequest) {
    List<Pattern> results = patternDao.findByOwner(
        owner.identifier().uuid(),
        pageRequest.pageSize() + 1,
        pageRequest.offset());
    return Page.of(results, pageRequest);
  }
}
