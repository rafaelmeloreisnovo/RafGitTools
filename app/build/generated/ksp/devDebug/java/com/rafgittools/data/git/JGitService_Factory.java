package com.rafgittools.data.git;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class JGitService_Factory implements Factory<JGitService> {
  @Override
  public JGitService get() {
    return newInstance();
  }

  public static JGitService_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static JGitService newInstance() {
    return new JGitService();
  }

  private static final class InstanceHolder {
    private static final JGitService_Factory INSTANCE = new JGitService_Factory();
  }
}
