package com.codeheadsystems.motif.server;

import com.codeheadsystems.motif.server.dagger.DaggerMotifComponent;
import com.codeheadsystems.motif.server.dagger.MotifComponent;
import com.codeheadsystems.motif.server.dagger.MotifModule;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;

public class MotifApplication extends Application<MotifConfiguration> {

  public static void main(String[] args) throws Exception {
    new MotifApplication().run(args);
  }

  @Override
  public String getName() {
    return "motif";
  }

  @Override
  public void initialize(Bootstrap<MotifConfiguration> bootstrap) {
    // initialization logic here
  }

  @Override
  public void run(MotifConfiguration configuration, Environment environment) {
    MotifComponent component = DaggerMotifComponent.builder()
        .motifModule(new MotifModule(configuration))
        .build();
    environment.jersey().register(component.helloWorldResource());
  }
}
