package com.fluxer.client.di;

import com.fluxer.client.data.local.SecureCookieStorage;
import com.fluxer.client.data.remote.AuthAuthenticator;
import com.fluxer.client.data.remote.AuthInterceptor;
import com.fluxer.client.data.remote.BaseUrlOverrideInterceptor;
import com.fluxer.client.data.remote.CsrfInterceptor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

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
public final class NetworkModule_ProvideOkHttpClientFactory implements Factory<OkHttpClient> {
  private final Provider<SecureCookieStorage> cookieStorageProvider;

  private final Provider<CsrfInterceptor> csrfInterceptorProvider;

  private final Provider<BaseUrlOverrideInterceptor> baseUrlOverrideInterceptorProvider;

  private final Provider<AuthInterceptor> authInterceptorProvider;

  private final Provider<HttpLoggingInterceptor> loggingInterceptorProvider;

  private final Provider<AuthAuthenticator> authAuthenticatorProvider;

  public NetworkModule_ProvideOkHttpClientFactory(
      Provider<SecureCookieStorage> cookieStorageProvider,
      Provider<CsrfInterceptor> csrfInterceptorProvider,
      Provider<BaseUrlOverrideInterceptor> baseUrlOverrideInterceptorProvider,
      Provider<AuthInterceptor> authInterceptorProvider,
      Provider<HttpLoggingInterceptor> loggingInterceptorProvider,
      Provider<AuthAuthenticator> authAuthenticatorProvider) {
    this.cookieStorageProvider = cookieStorageProvider;
    this.csrfInterceptorProvider = csrfInterceptorProvider;
    this.baseUrlOverrideInterceptorProvider = baseUrlOverrideInterceptorProvider;
    this.authInterceptorProvider = authInterceptorProvider;
    this.loggingInterceptorProvider = loggingInterceptorProvider;
    this.authAuthenticatorProvider = authAuthenticatorProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideOkHttpClient(cookieStorageProvider.get(), csrfInterceptorProvider.get(), baseUrlOverrideInterceptorProvider.get(), authInterceptorProvider.get(), loggingInterceptorProvider.get(), authAuthenticatorProvider.get());
  }

  public static NetworkModule_ProvideOkHttpClientFactory create(
      Provider<SecureCookieStorage> cookieStorageProvider,
      Provider<CsrfInterceptor> csrfInterceptorProvider,
      Provider<BaseUrlOverrideInterceptor> baseUrlOverrideInterceptorProvider,
      Provider<AuthInterceptor> authInterceptorProvider,
      Provider<HttpLoggingInterceptor> loggingInterceptorProvider,
      Provider<AuthAuthenticator> authAuthenticatorProvider) {
    return new NetworkModule_ProvideOkHttpClientFactory(cookieStorageProvider, csrfInterceptorProvider, baseUrlOverrideInterceptorProvider, authInterceptorProvider, loggingInterceptorProvider, authAuthenticatorProvider);
  }

  public static OkHttpClient provideOkHttpClient(SecureCookieStorage cookieStorage,
      CsrfInterceptor csrfInterceptor, BaseUrlOverrideInterceptor baseUrlOverrideInterceptor,
      AuthInterceptor authInterceptor, HttpLoggingInterceptor loggingInterceptor,
      AuthAuthenticator authAuthenticator) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideOkHttpClient(cookieStorage, csrfInterceptor, baseUrlOverrideInterceptor, authInterceptor, loggingInterceptor, authAuthenticator));
  }
}
