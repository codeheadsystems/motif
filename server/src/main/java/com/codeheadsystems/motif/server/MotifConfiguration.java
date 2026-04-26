package com.codeheadsystems.motif.server;

import com.codeheadsystems.hofmann.dropwizard.HofmannConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MotifConfiguration extends HofmannConfiguration {

  @JsonProperty
  private String databaseUrl;

  @JsonProperty
  private String databaseUser;

  @JsonProperty
  private String databasePassword;

  /**
   * How often the pattern detector sweeps every owner. Default: 24h.
   * Lower this in dev to dogfood the detector quickly.
   */
  @JsonProperty
  private long patternDetectionIntervalSeconds = 86_400L;

  public String getDatabaseUrl() {
    return databaseUrl;
  }

  public String getDatabaseUser() {
    return databaseUser;
  }

  public String getDatabasePassword() {
    return databasePassword;
  }

  public long getPatternDetectionIntervalSeconds() {
    return patternDetectionIntervalSeconds;
  }

  public void setPatternDetectionIntervalSeconds(long patternDetectionIntervalSeconds) {
    this.patternDetectionIntervalSeconds = patternDetectionIntervalSeconds;
  }
}
