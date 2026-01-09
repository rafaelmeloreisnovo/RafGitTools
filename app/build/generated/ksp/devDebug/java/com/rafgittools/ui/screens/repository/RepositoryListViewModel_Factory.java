package com.rafgittools.ui.screens.repository;

import com.rafgittools.domain.repository.GitRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class RepositoryListViewModel_Factory implements Factory<RepositoryListViewModel> {
  private final Provider<GitRepository> gitRepositoryProvider;

  public RepositoryListViewModel_Factory(Provider<GitRepository> gitRepositoryProvider) {
    this.gitRepositoryProvider = gitRepositoryProvider;
  }

  @Override
  public RepositoryListViewModel get() {
    return newInstance(gitRepositoryProvider.get());
  }

  public static RepositoryListViewModel_Factory create(
      Provider<GitRepository> gitRepositoryProvider) {
    return new RepositoryListViewModel_Factory(gitRepositoryProvider);
  }

  public static RepositoryListViewModel newInstance(GitRepository gitRepository) {
    return new RepositoryListViewModel(gitRepository);
  }
}
