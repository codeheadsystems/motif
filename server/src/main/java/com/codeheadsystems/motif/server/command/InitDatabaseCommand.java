package com.codeheadsystems.motif.server.command;

import com.codeheadsystems.motif.server.MotifConfiguration;
import io.dropwizard.core.cli.ConfiguredCommand;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dropwizard CLI command that applies Flyway migrations to the configured database.
 * <p>
 * Usage: {@code java -jar motif.jar init-db config.yml}
 * <p>
 * Secrets (OPAQUE seeds, OPRF master key, JWT signing secret) are no longer generated or
 * stored by this command — they are sourced from environment variables at server startup.
 * Run this command before launching the server against a fresh database, or any time a
 * new Flyway migration is added.
 */
public class InitDatabaseCommand extends ConfiguredCommand<MotifConfiguration> {

  private static final Logger log = LoggerFactory.getLogger(InitDatabaseCommand.class);

  public InitDatabaseCommand() {
    super("init-db", "Apply Flyway migrations to the configured database");
  }

  @Override
  protected void run(Bootstrap<MotifConfiguration> bootstrap,
                     Namespace namespace,
                     MotifConfiguration configuration) {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(configuration.getDatabaseUrl());
    ds.setUser(configuration.getDatabaseUser());
    ds.setPassword(configuration.getDatabasePassword());

    log.info("Applying Flyway migrations to {}", configuration.getDatabaseUrl());
    Flyway.configure().dataSource(ds).load().migrate();
    log.info("Migrations complete.");
  }
}
