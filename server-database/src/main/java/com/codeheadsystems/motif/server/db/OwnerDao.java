package com.codeheadsystems.motif.server.db;

import com.codeheadsystems.motif.model.Identifier;
import com.codeheadsystems.motif.model.Owner;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@RegisterRowMapper(OwnerDao.OwnerRowMapper.class)
public interface OwnerDao {

  @SqlUpdate("INSERT INTO owners (uuid, value) "
      + "VALUES (:uuid, :value) "
      + "ON CONFLICT (uuid) DO UPDATE SET "
      + "value = EXCLUDED.value")
  void upsert(@Bind("uuid") UUID uuid, @Bind("value") String value);

  @SqlQuery("SELECT * FROM owners WHERE uuid = :uuid")
  Optional<Owner> findByIdentifier(@Bind("uuid") UUID uuid);

  @SqlUpdate("DELETE FROM owners WHERE uuid = :uuid")
  int deleteByIdentifier(@Bind("uuid") UUID uuid);

  @SqlQuery("SELECT * FROM owners WHERE value = :value")
  Optional<Owner> findByValue(@Bind("value") String value);

  default void store(Owner owner) {
    upsert(owner.identifier().uuid(), owner.value());
  }

  default Optional<Owner> get(Identifier identifier) {
    return findByIdentifier(identifier.uuid());
  }

  default boolean delete(Identifier identifier) {
    return deleteByIdentifier(identifier.uuid()) > 0;
  }

  default Optional<Owner> find(String value) {
    return findByValue(value.strip().toUpperCase());
  }

  class OwnerRowMapper implements RowMapper<Owner> {
    @Override
    public Owner map(ResultSet rs, StatementContext ctx) throws SQLException {
      return Owner.builder()
          .value(rs.getString("value"))
          .identifier(new Identifier(rs.getObject("uuid", UUID.class)))
          .build();
    }
  }

}
