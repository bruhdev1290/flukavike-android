package com.fluxer.client.di;

import android.content.Context;
import com.fluxer.client.data.local.SecureCookieStorage;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class NetworkModule_ProvideSecureCookieStorageFactory implements Factory<SecureCookieStorage> {
  private final Provider<Context> contextProvider;

  public NetworkModule_ProvideSecureCookieStorageFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SecureCookieStorage get() {
    return provideSecureCookieStorage(contextProvider.get());
  }

  public static NetworkModule_ProvideSecureCookieStorageFactory create(
      Provider<Context> contextProvider) {
    return new NetworkModule_ProvideSecureCookieStorageFactory(contextProvider);
  }

  public static SecureCookieStorage provideSecureCookieStorage(Context context) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideSecureCookieStorage(context));
  }
}
