package com.rafgittools.data.repository;

import com.rafgittools.data.git.JGitService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class GitRepositoryImpl_Factory implements Factory<GitRepositoryImpl> {
  private final Provider<JGitService> jGitServiceProvider;

  public GitRepositoryImpl_Factory(Provider<JGitService> jGitServiceProvider) {
    this.jGitServiceProvider = jGitServiceProvider;
  }

  @Override
  public GitRepositoryImpl get() {
    return newInstance(jGitServiceProvider.get());
  }

  public static GitRepositoryImpl_Factory create(Provider<JGitService> jGitServiceProvider) {
    return new GitRepositoryImpl_Factory(jGitServiceProvider);
  }

  public static GitRepositoryImpl newInstance(JGitService jGitService) {
    return new GitRepositoryImpl(jGitService);
  }
}
