package com.codeheadsystems.motif.server.dagger;

import com.codeheadsystems.motif.server.resource.HelloWorldResource;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = {MotifModule.class})
public interface MotifComponent {

  HelloWorldResource helloWorldResource();

  @Component.Builder
  interface Builder {
    Builder motifModule(MotifModule module);
    MotifComponent build();
  }
}
