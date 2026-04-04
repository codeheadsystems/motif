package com.codeheadsystems.motif.server;

import com.codeheadsystems.hofmann.dropwizard.HofmannBundle;
import com.codeheadsystems.motif.server.command.InitDatabaseCommand;
import com.codeheadsystems.motif.server.dagger.DaggerMotifComponent;
import com.codeheadsystems.motif.server.dagger.MotifComponent;
import com.codeheadsystems.motif.server.dagger.MotifModule;
import com.codeheadsystems.motif.server.store.JdbiCredentialStore;
import com.codeheadsystems.motif.server.store.JdbiPendingSessionStore;
import com.codeheadsystems.motif.server.store.JdbiSessionStore;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;

public class MotifApplication extends Application<MotifConfiguration> {

  private final JdbiCredentialStore credentialStore = new JdbiCredentialStore();
  private final JdbiSessionStore sessionStore = new JdbiSessionStore();
  private final JdbiPendingSessionStore pendingSessionStore = new JdbiPendingSessionStore();

  public static void main(String[] args) throws Exception {
    new MotifApplication().run(args);
  }

  @Override
  public String getName() {
    return "motif";
  }

  @Override
  public void initialize(Bootstrap<MotifConfiguration> bootstrap) {
    bootstrap.addCommand(new InitDatabaseCommand());
    // SetupBundle runs first — initializes JDBI, stores, and loads DB config
    bootstrap.addBundle(new SetupBundle(credentialStore, sessionStore, pendingSessionStore));
    // HofmannBundle runs second — stores are already initialized
    bootstrap.addBundle(new HofmannBundle<>(credentialStore, sessionStore, null));
  }

  @Override
  public void run(MotifConfiguration configuration, Environment environment) {
    MotifComponent component = DaggerMotifComponent.builder()
        .motifModule(new MotifModule(configuration))
        .build();
    environment.jersey().register(component.helloWorldResource());
  }
}
