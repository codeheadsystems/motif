package com.codeheadsystems.motif.server.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldResource {

  @Inject
  public HelloWorldResource() {
  }

  @GET
  public String hello() {
    return "{\"message\":\"Hello, World!\"}";
  }
}
