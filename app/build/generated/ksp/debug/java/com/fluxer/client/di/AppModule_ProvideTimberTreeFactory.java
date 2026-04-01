package com.fluxer.client.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import timber.log.Timber;

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
public final class AppModule_ProvideTimberTreeFactory implements Factory<Timber.Tree> {
  @Override
  public Timber.Tree get() {
    return provideTimberTree();
  }

  public static AppModule_ProvideTimberTreeFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static Timber.Tree provideTimberTree() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideTimberTree());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideTimberTreeFactory INSTANCE = new AppModule_ProvideTimberTreeFactory();
  }
}
