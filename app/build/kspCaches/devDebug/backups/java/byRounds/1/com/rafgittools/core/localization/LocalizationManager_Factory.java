package com.rafgittools.core.localization;

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
public final class LocalizationManager_Factory implements Factory<LocalizationManager> {
  @Override
  public LocalizationManager get() {
    return newInstance();
  }

  public static LocalizationManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static LocalizationManager newInstance() {
    return new LocalizationManager();
  }

  private static final class InstanceHolder {
    private static final LocalizationManager_Factory INSTANCE = new LocalizationManager_Factory();
  }
}
