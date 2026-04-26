package com.codeheadsystems.motif.server.dagger;

import com.codeheadsystems.motif.server.db.manager.PatternDetectionManager;
import com.codeheadsystems.motif.server.resource.CategoryResource;
import com.codeheadsystems.motif.server.resource.EventResource;
import com.codeheadsystems.motif.server.resource.HelloWorldResource;
import com.codeheadsystems.motif.server.resource.NoteResource;
import com.codeheadsystems.motif.server.resource.OwnerResource;
import com.codeheadsystems.motif.server.resource.PatternResource;
import com.codeheadsystems.motif.server.resource.SubjectResource;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = {MotifModule.class})
public interface MotifComponent {

  HelloWorldResource helloWorldResource();

  OwnerResource ownerResource();

  SubjectResource subjectResource();

  CategoryResource categoryResource();

  EventResource eventResource();

  NoteResource noteResource();

  PatternResource patternResource();

  PatternDetectionManager patternDetectionManager();

  @Component.Builder
  interface Builder {
    Builder motifModule(MotifModule module);
    MotifComponent build();
  }
}
