package com.codeheadsystems.motif.server;

import com.codeheadsystems.hofmann.dropwizard.HofmannBundle;
import com.codeheadsystems.motif.server.command.InitDatabaseCommand;
import com.codeheadsystems.motif.server.dagger.DaggerMotifComponent;
import com.codeheadsystems.motif.server.dagger.MotifComponent;
import com.codeheadsystems.motif.server.dagger.MotifModule;
import com.codeheadsystems.motif.server.resource.SessionResource;
import com.codeheadsystems.motif.server.db.dao.OpaquePendingSessionDao;
import com.codeheadsystems.motif.server.db.dao.OpaqueSessionDao;
import com.codeheadsystems.motif.server.store.JdbiCredentialStore;
import com.codeheadsystems.motif.server.store.JdbiPendingSessionStore;
import com.codeheadsystems.motif.server.store.JdbiSessionStore;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.servlets.assets.AssetServlet;
import org.eclipse.jetty.server.handler.ErrorHandler;
import jakarta.servlet.DispatcherType;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

public class MotifApplication extends Application<MotifConfiguration> {

  private final JdbiCredentialStore credentialStore = new JdbiCredentialStore();
  private final JdbiSessionStore sessionStore = new JdbiSessionStore();
  private final JdbiPendingSessionStore pendingSessionStore = new JdbiPendingSessionStore();
  private final SetupBundle setupBundle = new SetupBundle(credentialStore, sessionStore, pendingSessionStore);

  public static void main(String[] args) throws Exception {
    new MotifApplication().run(args);
  }

  @Override
  public String getName() {
    return "motif";
  }

  @Override
  public void initialize(Bootstrap<MotifConfiguration> bootstrap) {
    bootstrap.getObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    bootstrap.addCommand(new InitDatabaseCommand());
    // SetupBundle runs first — initializes JDBI, stores, and loads DB config
    bootstrap.addBundle(setupBundle);
    // HofmannBundle runs second — stores are already initialized
    bootstrap.addBundle(new HofmannBundle<>(credentialStore, sessionStore, null));
  }

  @Override
  public void run(MotifConfiguration configuration, Environment environment) {
    MotifComponent component = DaggerMotifComponent.builder()
        .motifModule(new MotifModule(configuration, setupBundle.getJdbi()))
        .build();

    environment.jersey().register(component.helloWorldResource());
    environment.jersey().register(component.ownerResource());
    environment.jersey().register(component.subjectResource());
    environment.jersey().register(component.eventResource());
    environment.jersey().register(component.noteResource());
    environment.jersey().register(new SessionResource());

    // Suppress detailed Jetty error pages to avoid leaking server information
    ErrorHandler errorHandler = new ErrorHandler();
    errorHandler.setShowStacks(false);
    errorHandler.setShowMessageInTitle(false);
    environment.getApplicationContext().setErrorHandler(errorHandler);

    // Session cleanup task
    OpaqueSessionDao sessionDao = setupBundle.getJdbi().onDemand(OpaqueSessionDao.class);
    OpaquePendingSessionDao pendingSessionDao = setupBundle.getJdbi().onDemand(OpaquePendingSessionDao.class);
    environment.lifecycle().manage(new SessionCleanupTask(sessionDao, pendingSessionDao));

    // Cookie-to-Authorization bridge (must run before HofmannBundle's auth filter)
    environment.servlets()
        .addFilter("cookie-auth", new CookieAuthFilter())
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/api/*");

    // Security headers for all responses (including static assets)
    environment.servlets()
        .addFilter("security-headers", new SecurityHeadersFilter())
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

    // Serve webapp static assets at /app
    environment.servlets()
        .addServlet("webapp", new AssetServlet(
            "/assets", "/app", "index.html", StandardCharsets.UTF_8))
        .addMapping("/app", "/app/*");
  }
}
