package com.codeheadsystems.motif.server.db.model;

import java.time.Duration;

/**
 * Classification of a pattern's recurrence period. Bands are intentionally generous so that
 * "every Sunday" (7d) and "every other Sunday" (14d) both classify cleanly without splitting
 * into too many buckets. Anything outside the named bands is OTHER (still surfaced, just
 * without a friendly label).
 */
public enum PeriodClassification {
  DAILY,    // mean interval in [12h, 36h]
  WEEKLY,   // mean interval in [5d, 9d]
  MONTHLY,  // mean interval in [25d, 35d]
  OTHER;

  public static PeriodClassification fromMeanInterval(Duration mean) {
    long seconds = mean.getSeconds();
    if (seconds >= 12L * 3600 && seconds <= 36L * 3600) return DAILY;
    if (seconds >= 5L * 86400 && seconds <= 9L * 86400) return WEEKLY;
    if (seconds >= 25L * 86400 && seconds <= 35L * 86400) return MONTHLY;
    return OTHER;
  }
}
