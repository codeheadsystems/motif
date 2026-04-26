package com.codeheadsystems.motif.server.resource;

import com.codeheadsystems.motif.server.db.manager.TierRequiredException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Maps a {@link TierRequiredException} thrown anywhere in the request handler to a
 * 402 Payment Required response. The body identifies which tier is needed so the
 * webapp can present a relevant upsell.
 */
@Provider
public class TierRequiredExceptionMapper implements ExceptionMapper<TierRequiredException> {
  @Override
  public Response toResponse(TierRequiredException ex) {
    return Response.status(402)
        .type(MediaType.APPLICATION_JSON)
        .entity(Map.of(
            "error", "tier_required",
            "required", ex.required().name(),
            "actual", ex.actual().name(),
            "message", ex.getMessage()))
        .build();
  }
}
