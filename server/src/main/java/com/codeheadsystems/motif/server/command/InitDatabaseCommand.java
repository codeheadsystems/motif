package com.codeheadsystems.motif.server.command;

import com.codeheadsystems.motif.server.MotifConfiguration;
import com.codeheadsystems.motif.server.db.dao.ConfigurationValueDao;
import io.dropwizard.core.cli.ConfiguredCommand;
import io.dropwizard.core.setup.Bootstrap;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import net.sourceforge.argparse4j.inf.Namespace;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dropwizard CLI command that initializes the database with randomly generated OPAQUE keys.
 * <p>
 * Usage: {@code java -jar motif.jar init-db config.yml}
 * <p>
 * Generates and stores the following keys in the {@code configuration_values} table:
 * <ul>
 *   <li>{@code hofmann.serverKeySeedHex} — 32-byte OPAQUE server key seed</li>
 *   <li>{@code hofmann.oprfSeedHex} — 32-byte OPRF seed</li>
 *   <li>{@code hofmann.oprfMasterKeyHex} — 32-byte OPRF master key</li>
 *   <li>{@code hofmann.jwtSecretHex} — 32-byte JWT signing secret</li>
 *   <li>{@code hofmann.context} — OPAQUE protocol context string</li>
 * </ul>
 * Existing keys are not overwritten (idempotent).
 */
public class InitDatabaseCommand extends ConfiguredCommand<MotifConfiguration> {

  private static final Logger log = LoggerFactory.getLogger(InitDatabaseCommand.class);
  private static final int KEY_BYTES = 32;
  private static final String DEFAULT_CONTEXT = "motif-opaque-v1";

  private static final List<String> HEX_KEYS = List.of(
      "hofmann.serverKeySeedHex",
      "hofmann.oprfSeedHex",
      "hofmann.oprfMasterKeyHex",
      "hofmann.jwtSecretHex"
  );

  public InitDatabaseCommand() {
    super("init-db", "Initialize the database with OPAQUE configuration keys");
  }

  @Override
  protected void run(Bootstrap<MotifConfiguration> bootstrap,
                     Namespace namespace,
                     MotifConfiguration configuration) {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(configuration.getDatabaseUrl());
    ds.setUser(configuration.getDatabaseUser());
    ds.setPassword(configuration.getDatabasePassword());

    Flyway.configure().dataSource(ds).load().migrate();

    Jdbi jdbi = Jdbi.create(ds);
    jdbi.installPlugin(new SqlObjectPlugin());
    ConfigurationValueDao configDao = jdbi.onDemand(ConfigurationValueDao.class);

    SecureRandom random = new SecureRandom();
    HexFormat hex = HexFormat.of();

    for (String key : HEX_KEYS) {
      if (configDao.findByKey(key).isEmpty()) {
        byte[] value = new byte[KEY_BYTES];
        random.nextBytes(value);
        configDao.upsert(key, hex.formatHex(value));
        log.info("Generated and stored: {}", key);
      } else {
        log.info("Already exists, skipping: {}", key);
      }
    }

    if (configDao.findByKey("hofmann.context").isEmpty()) {
      configDao.upsert("hofmann.context", DEFAULT_CONTEXT);
      log.info("Stored default context: {}", DEFAULT_CONTEXT);
    } else {
      log.info("Already exists, skipping: hofmann.context");
    }

    log.info("Database initialization complete.");
  }
}
