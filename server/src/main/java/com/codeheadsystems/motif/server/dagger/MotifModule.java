package com.codeheadsystems.motif.server.dagger;

import com.codeheadsystems.motif.server.MotifConfiguration;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class MotifModule {

  private final MotifConfiguration configuration;

  public MotifModule(final MotifConfiguration configuration) {
    this.configuration = configuration;
  }

  @Provides
  @Singleton
  MotifConfiguration configuration() {
    return configuration;
  }
}
