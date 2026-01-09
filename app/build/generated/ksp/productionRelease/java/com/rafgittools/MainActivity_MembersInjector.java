package com.rafgittools;

import com.rafgittools.core.localization.LocalizationManager;
import com.rafgittools.data.preferences.PreferencesRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<LocalizationManager> localizationManagerProvider;

  private final Provider<PreferencesRepository> preferencesRepositoryProvider;

  public MainActivity_MembersInjector(Provider<LocalizationManager> localizationManagerProvider,
      Provider<PreferencesRepository> preferencesRepositoryProvider) {
    this.localizationManagerProvider = localizationManagerProvider;
    this.preferencesRepositoryProvider = preferencesRepositoryProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<LocalizationManager> localizationManagerProvider,
      Provider<PreferencesRepository> preferencesRepositoryProvider) {
    return new MainActivity_MembersInjector(localizationManagerProvider, preferencesRepositoryProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectLocalizationManager(instance, localizationManagerProvider.get());
    injectPreferencesRepository(instance, preferencesRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.rafgittools.MainActivity.localizationManager")
  public static void injectLocalizationManager(MainActivity instance,
      LocalizationManager localizationManager) {
    instance.localizationManager = localizationManager;
  }

  @InjectedFieldSignature("com.rafgittools.MainActivity.preferencesRepository")
  public static void injectPreferencesRepository(MainActivity instance,
      PreferencesRepository preferencesRepository) {
    instance.preferencesRepository = preferencesRepository;
  }
}
