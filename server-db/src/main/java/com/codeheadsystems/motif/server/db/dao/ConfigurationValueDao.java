package com.codeheadsystems.motif.server.db.dao;

import com.codeheadsystems.motif.server.db.model.ConfigurationValue;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@RegisterRowMapper(ConfigurationValueDao.ConfigurationValueRowMapper.class)
public interface ConfigurationValueDao {

  @SqlUpdate("INSERT INTO configuration_values (key, value) "
      + "VALUES (:key, :value) "
      + "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value")
  void upsert(@Bind("key") String key, @Bind("value") String value);

  @SqlQuery("SELECT * FROM configuration_values WHERE key = :key")
  Optional<ConfigurationValue> findByKey(@Bind("key") String key);

  @SqlUpdate("DELETE FROM configuration_values WHERE key = :key")
  int deleteByKey(@Bind("key") String key);

  class ConfigurationValueRowMapper implements RowMapper<ConfigurationValue> {
    @Override
    public ConfigurationValue map(ResultSet rs, StatementContext ctx) throws SQLException {
      return new ConfigurationValue(rs.getString("key"), rs.getString("value"));
    }
  }
}
