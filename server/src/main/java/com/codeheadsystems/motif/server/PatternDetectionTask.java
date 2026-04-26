package com.codeheadsystems.motif.server;

import com.codeheadsystems.motif.server.db.manager.PatternDetectionManager;
import io.dropwizard.lifecycle.Managed;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dropwizard {@link Managed} that runs the pattern detector across all owners on a fixed
 * cadence. Default 24h; configurable via {@code patternDetectionIntervalSeconds} in the
 * application YAML so dev / tests can shorten the loop.
 *
 * <p>Single-thread executor: detector runs are CPU-light per owner and serialized to keep
 * DB load predictable. The first run fires {@code intervalSeconds} after boot, not at
 * startup, so a fast restart loop doesn't repeatedly pummel the DB.
 */
public class PatternDetectionTask implements Managed {

  private static final Logger log = LoggerFactory.getLogger(PatternDetectionTask.class);

  private final PatternDetectionManager detectionManager;
  private final long intervalSeconds;
  private ScheduledExecutorService executor;

  public PatternDetectionTask(PatternDetectionManager detectionManager, long intervalSeconds) {
    if (intervalSeconds <= 0) {
      throw new IllegalArgumentException("intervalSeconds must be positive");
    }
    this.detectionManager = detectionManager;
    this.intervalSeconds = intervalSeconds;
  }

  @Override
  public void start() {
    log.info("Starting pattern detection task — interval {}",
        Duration.ofSeconds(intervalSeconds));
    executor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "pattern-detection");
      t.setDaemon(true);
      return t;
    });
    executor.scheduleAtFixedRate(this::runSweep, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  private void runSweep() {
    try {
      detectionManager.detectForAllOwners();
    } catch (Throwable t) {
      // Catch Throwable so a bug in the sweep doesn't kill the scheduled executor;
      // ScheduledExecutorService silently cancels future runs if a task throws.
      log.error("Pattern detection sweep crashed", t);
    }
  }
}
