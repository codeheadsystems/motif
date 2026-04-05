package com.codeheadsystems.motif.server;

import com.codeheadsystems.motif.server.db.dao.OpaquePendingSessionDao;
import com.codeheadsystems.motif.server.db.dao.OpaqueSessionDao;
import io.dropwizard.lifecycle.Managed;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically cleans up expired sessions and pending sessions from the database.
 */
public class SessionCleanupTask implements Managed {

  private static final Logger LOG = LoggerFactory.getLogger(SessionCleanupTask.class);
  private static final long CLEANUP_INTERVAL_MINUTES = 15;
  private static final long PENDING_SESSION_TTL_MINUTES = 10;

  private final OpaqueSessionDao sessionDao;
  private final OpaquePendingSessionDao pendingSessionDao;
  private ScheduledExecutorService scheduler;

  public SessionCleanupTask(OpaqueSessionDao sessionDao, OpaquePendingSessionDao pendingSessionDao) {
    this.sessionDao = sessionDao;
    this.pendingSessionDao = pendingSessionDao;
  }

  @Override
  public void start() {
    scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "session-cleanup");
      t.setDaemon(true);
      return t;
    });
    scheduler.scheduleAtFixedRate(this::cleanup,
        CLEANUP_INTERVAL_MINUTES, CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
    LOG.info("Session cleanup task scheduled every {} minutes", CLEANUP_INTERVAL_MINUTES);
  }

  @Override
  public void stop() {
    if (scheduler != null) {
      scheduler.shutdownNow();
    }
  }

  private void cleanup() {
    try {
      OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
      int expiredSessions = sessionDao.deleteExpired(now);
      OffsetDateTime pendingCutoff = now.minusMinutes(PENDING_SESSION_TTL_MINUTES);
      int expiredPending = pendingSessionDao.deleteExpired(pendingCutoff);
      if (expiredSessions > 0 || expiredPending > 0) {
        LOG.info("Session cleanup: removed {} expired sessions, {} expired pending sessions",
            expiredSessions, expiredPending);
      }
    } catch (Exception e) {
      LOG.warn("Session cleanup failed", e);
    }
  }
}
