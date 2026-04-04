package com.codeheadsystems.motif.server;

import com.codeheadsystems.motif.server.db.dao.ConfigurationValueDao;
import com.codeheadsystems.motif.server.db.dao.OpaqueCredentialDao;
import com.codeheadsystems.motif.server.db.dao.OpaquePendingSessionDao;
import com.codeheadsystems.motif.server.db.dao.OpaqueSessionDao;
import com.codeheadsystems.motif.server.db.model.ConfigurationValue;
import com.codeheadsystems.motif.server.store.JdbiCredentialStore;
import com.codeheadsystems.motif.server.store.JdbiPendingSessionStore;
import com.codeheadsystems.motif.server.store.JdbiSessionStore;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registered before HofmannBundle to initialize JDBI, stores, and load config from the database.
 * If no OPAQUE keys exist in the database, generates random ones automatically.
 */
public class SetupBundle implements ConfiguredBundle<MotifConfiguration> {

  private static final Logger log = LoggerFactory.getLogger(SetupBundle.class);
  private static final int KEY_BYTES = 32;
  private static final String DEFAULT_CONTEXT = "motif-opaque-v1";
  private static final List<String> HEX_KEYS = List.of(
      "hofmann.serverKeySeedHex",
      "hofmann.oprfSeedHex",
      "hofmann.oprfMasterKeyHex",
      "hofmann.jwtSecretHex"
  );

  private final JdbiCredentialStore credentialStore;
  private final JdbiSessionStore sessionStore;
  private final JdbiPendingSessionStore pendingSessionStore;

  public SetupBundle(JdbiCredentialStore credentialStore,
                     JdbiSessionStore sessionStore,
                     JdbiPendingSessionStore pendingSessionStore) {
    this.credentialStore = credentialStore;
    this.sessionStore = sessionStore;
    this.pendingSessionStore = pendingSessionStore;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // no-op
  }

  @Override
  public void run(MotifConfiguration configuration, Environment environment) {
    DataSource dataSource = buildDataSource(configuration);

    Flyway.configure().dataSource(dataSource).load().migrate();

    Jdbi jdbi = Jdbi.create(dataSource);
    jdbi.installPlugin(new SqlObjectPlugin());

    // Initialize stores with DAOs
    credentialStore.initialize(jdbi.onDemand(OpaqueCredentialDao.class));
    sessionStore.initialize(jdbi.onDemand(OpaqueSessionDao.class));
    pendingSessionStore.initialize(jdbi.onDemand(OpaquePendingSessionDao.class));

    // Ensure OPAQUE keys exist, generating if needed
    ConfigurationValueDao configDao = jdbi.onDemand(ConfigurationValueDao.class);
    ensureKeysExist(configDao);
    loadConfigFromDatabase(configuration, configDao);
  }

  private void ensureKeysExist(ConfigurationValueDao configDao) {
    SecureRandom random = new SecureRandom();
    HexFormat hex = HexFormat.of();

    for (String key : HEX_KEYS) {
      if (configDao.findByKey(key).isEmpty()) {
        byte[] value = new byte[KEY_BYTES];
        random.nextBytes(value);
        configDao.upsert(key, hex.formatHex(value));
        log.warn("Generated missing key: {} — run 'init-db' for production deployments", key);
      }
    }
    if (configDao.findByKey("hofmann.context").isEmpty()) {
      configDao.upsert("hofmann.context", DEFAULT_CONTEXT);
      log.warn("Generated missing context: hofmann.context={}", DEFAULT_CONTEXT);
    }
  }

  private void loadConfigFromDatabase(MotifConfiguration configuration, ConfigurationValueDao configDao) {
    configDao.findByKey("hofmann.serverKeySeedHex")
        .map(ConfigurationValue::value)
        .ifPresent(configuration::setServerKeySeedHex);
    configDao.findByKey("hofmann.oprfSeedHex")
        .map(ConfigurationValue::value)
        .ifPresent(configuration::setOprfSeedHex);
    configDao.findByKey("hofmann.oprfMasterKeyHex")
        .map(ConfigurationValue::value)
        .ifPresent(configuration::setOprfMasterKeyHex);
    configDao.findByKey("hofmann.jwtSecretHex")
        .map(ConfigurationValue::value)
        .ifPresent(configuration::setJwtSecretHex);
    configDao.findByKey("hofmann.context")
        .map(ConfigurationValue::value)
        .ifPresent(configuration::setContext);
  }

  private DataSource buildDataSource(MotifConfiguration configuration) {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(configuration.getDatabaseUrl());
    ds.setUser(configuration.getDatabaseUser());
    ds.setPassword(configuration.getDatabasePassword());
    return ds;
  }
}
