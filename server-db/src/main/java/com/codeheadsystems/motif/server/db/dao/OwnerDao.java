package com.codeheadsystems.motif.server.db.dao;

import com.codeheadsystems.motif.server.db.model.Identifier;
import com.codeheadsystems.motif.server.db.model.Owner;
import com.codeheadsystems.motif.server.db.model.Tier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
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

  @SqlUpdate("INSERT INTO owners (uuid, value, deleted, tier) "
      + "VALUES (:uuid, :value, :deleted, :tier) "
      + "ON CONFLICT (uuid) DO UPDATE SET "
      + "value = EXCLUDED.value, "
      + "deleted = EXCLUDED.deleted, "
      + "tier = EXCLUDED.tier")
  void upsert(@Bind("uuid") UUID uuid, @Bind("value") String value,
              @Bind("deleted") boolean deleted, @Bind("tier") String tier);

  /** Backwards-compatible overload that defaults tier to FREE_SYNCED. */
  default void upsert(UUID uuid, String value, boolean deleted) {
    upsert(uuid, value, deleted, "FREE_SYNCED");
  }

  @SqlUpdate("INSERT INTO owners (uuid, value, deleted, tier) "
      + "VALUES (:uuid, :value, :deleted, :tier) "
      + "ON CONFLICT (value) DO NOTHING")
  void insertIfAbsentByValue(@Bind("uuid") UUID uuid, @Bind("value") String value,
                             @Bind("deleted") boolean deleted, @Bind("tier") String tier);

  /** Backwards-compatible overload that defaults tier to FREE_SYNCED. */
  default void insertIfAbsentByValue(UUID uuid, String value, boolean deleted) {
    insertIfAbsentByValue(uuid, value, deleted, "FREE_SYNCED");
  }

  @SqlUpdate("UPDATE owners SET tier = :tier WHERE uuid = :uuid AND deleted = false")
  int updateTier(@Bind("uuid") UUID uuid, @Bind("tier") String tier);

  @SqlQuery("SELECT * FROM owners WHERE uuid = :uuid AND deleted = false")
  Optional<Owner> findByIdentifier(@Bind("uuid") UUID uuid);

  @SqlQuery("SELECT * FROM owners WHERE uuid = :uuid")
  Optional<Owner> findByIdentifierIncludingDeleted(@Bind("uuid") UUID uuid);

  @SqlUpdate("UPDATE owners SET deleted = true WHERE uuid = :uuid AND deleted = false")
  int softDelete(@Bind("uuid") UUID uuid);

  @SqlUpdate("DELETE FROM owners WHERE uuid = :uuid AND deleted = true")
  int deleteByIdentifier(@Bind("uuid") UUID uuid);

  @SqlQuery("SELECT * FROM owners WHERE value = :value AND deleted = false")
  Optional<Owner> findByValue(@Bind("value") String value);

  @SqlQuery("SELECT * FROM owners WHERE value = :value")
  Optional<Owner> findByValueIncludingDeleted(@Bind("value") String value);

  /**
   * Returns every non-deleted owner. Used by the pattern detector's scheduled scan.
   * For Phase 2 MVP this is fine; once owner counts get large, switch to streaming
   * or to per-owner triggers (e.g. enqueue when an event is logged).
   */
  @SqlQuery("SELECT * FROM owners WHERE deleted = false")
  List<Owner> findAllActive();

  class OwnerRowMapper implements RowMapper<Owner> {
    @Override
    public Owner map(ResultSet rs, StatementContext ctx) throws SQLException {
      return Owner.builder()
          .value(rs.getString("value"))
          .identifier(new Identifier(rs.getObject("uuid", UUID.class)))
          .deleted(rs.getBoolean("deleted"))
          .tier(Tier.valueOf(rs.getString("tier")))
          .build();
    }
  }

}
