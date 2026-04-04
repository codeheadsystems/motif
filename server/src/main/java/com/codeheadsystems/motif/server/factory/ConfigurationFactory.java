package com.codeheadsystems.motif.server.factory;

import com.codeheadsystems.motif.common.Configuration;
import com.codeheadsystems.motif.server.dao.ConfigurationValueDao;
import com.codeheadsystems.motif.server.model.ConfigurationValue;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConfigurationFactory {

  private final ConfigurationValueDao configurationValueDao;

  @Inject
  public ConfigurationFactory(final ConfigurationValueDao configurationValueDao) {
    this.configurationValueDao = configurationValueDao;
  }

  public Configuration create() {
    return new Configuration(
        key -> configurationValueDao.findByKey(key)
            .map(ConfigurationValue::value)
            .orElse(null));
  }
}
