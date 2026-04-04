package com.codeheadsystems.motif.server.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.codeheadsystems.motif.common.Configuration;
import com.codeheadsystems.motif.server.dao.ConfigurationValueDao;
import com.codeheadsystems.motif.server.model.ConfigurationValue;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigurationFactoryTest {

  @Mock
  private ConfigurationValueDao configurationValueDao;

  private ConfigurationFactory configurationFactory;

  @BeforeEach
  void setUp() {
    configurationFactory = new ConfigurationFactory(configurationValueDao);
  }

  @Test
  void createReturnsConfigurationThatResolvesExistingKeys() {
    when(configurationValueDao.findByKey("db.host"))
        .thenReturn(Optional.of(new ConfigurationValue("db.host", "localhost")));

    Configuration configuration = configurationFactory.create();

    assertThat(configuration.apply("db.host")).contains("localhost");
  }

  @Test
  void createReturnsConfigurationThatReturnsEmptyForMissingKeys() {
    when(configurationValueDao.findByKey("missing"))
        .thenReturn(Optional.empty());

    Configuration configuration = configurationFactory.create();

    assertThat(configuration.apply("missing")).isEmpty();
  }

  @Test
  void createReturnsConfigurationWhereGetThrowsForMissingKeys() {
    when(configurationValueDao.findByKey("missing"))
        .thenReturn(Optional.empty());

    Configuration configuration = configurationFactory.create();

    assertThatThrownBy(() -> configuration.get("missing"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("missing");
  }
}
