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
public final class RafGitToolsApplication_MembersInjector implements MembersInjector<RafGitToolsApplication> {
  private final Provider<LocalizationManager> localizationManagerProvider;

  private final Provider<PreferencesRepository> preferencesRepositoryProvider;

  public RafGitToolsApplication_MembersInjector(
      Provider<LocalizationManager> localizationManagerProvider,
      Provider<PreferencesRepository> preferencesRepositoryProvider) {
    this.localizationManagerProvider = localizationManagerProvider;
    this.preferencesRepositoryProvider = preferencesRepositoryProvider;
  }

  public static MembersInjector<RafGitToolsApplication> create(
      Provider<LocalizationManager> localizationManagerProvider,
      Provider<PreferencesRepository> preferencesRepositoryProvider) {
    return new RafGitToolsApplication_MembersInjector(localizationManagerProvider, preferencesRepositoryProvider);
  }

  @Override
  public void injectMembers(RafGitToolsApplication instance) {
    injectLocalizationManager(instance, localizationManagerProvider.get());
    injectPreferencesRepository(instance, preferencesRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.rafgittools.RafGitToolsApplication.localizationManager")
  public static void injectLocalizationManager(RafGitToolsApplication instance,
      LocalizationManager localizationManager) {
    instance.localizationManager = localizationManager;
  }

  @InjectedFieldSignature("com.rafgittools.RafGitToolsApplication.preferencesRepository")
  public static void injectPreferencesRepository(RafGitToolsApplication instance,
      PreferencesRepository preferencesRepository) {
    instance.preferencesRepository = preferencesRepository;
  }
}
