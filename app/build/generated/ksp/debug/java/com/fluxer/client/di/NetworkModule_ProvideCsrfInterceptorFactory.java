package com.fluxer.client.di;

import com.fluxer.client.data.local.SecureCookieStorage;
import com.fluxer.client.data.remote.CsrfInterceptor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class NetworkModule_ProvideCsrfInterceptorFactory implements Factory<CsrfInterceptor> {
  private final Provider<SecureCookieStorage> cookieStorageProvider;

  public NetworkModule_ProvideCsrfInterceptorFactory(
      Provider<SecureCookieStorage> cookieStorageProvider) {
    this.cookieStorageProvider = cookieStorageProvider;
  }

  @Override
  public CsrfInterceptor get() {
    return provideCsrfInterceptor(cookieStorageProvider.get());
  }

  public static NetworkModule_ProvideCsrfInterceptorFactory create(
      Provider<SecureCookieStorage> cookieStorageProvider) {
    return new NetworkModule_ProvideCsrfInterceptorFactory(cookieStorageProvider);
  }

  public static CsrfInterceptor provideCsrfInterceptor(SecureCookieStorage cookieStorage) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideCsrfInterceptor(cookieStorage));
  }
}
