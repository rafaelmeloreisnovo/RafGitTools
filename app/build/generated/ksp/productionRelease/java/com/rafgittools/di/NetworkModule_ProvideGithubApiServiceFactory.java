package com.rafgittools.di;

import com.rafgittools.data.github.GithubApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideGithubApiServiceFactory implements Factory<GithubApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideGithubApiServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public GithubApiService get() {
    return provideGithubApiService(retrofitProvider.get());
  }

  public static NetworkModule_ProvideGithubApiServiceFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideGithubApiServiceFactory(retrofitProvider);
  }

  public static GithubApiService provideGithubApiService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideGithubApiService(retrofit));
  }
}
