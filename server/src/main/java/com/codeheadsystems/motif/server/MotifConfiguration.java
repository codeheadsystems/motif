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

  public String getDatabaseUrl() {
    return databaseUrl;
  }

  public String getDatabaseUser() {
    return databaseUser;
  }

  public String getDatabasePassword() {
    return databasePassword;
  }
}
