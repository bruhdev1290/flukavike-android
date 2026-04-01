package com.fluxer.client.data.repository;

import com.fluxer.client.data.local.SecureCookieStorage;
import com.fluxer.client.data.remote.CsrfInterceptor;
import com.fluxer.client.data.remote.FluxerApiService;
import com.fluxer.client.data.remote.GatewayWebSocketManager;
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
public final class AuthRepository_Factory implements Factory<AuthRepository> {
  private final Provider<FluxerApiService> apiServiceProvider;

  private final Provider<SecureCookieStorage> cookieStorageProvider;

  private final Provider<CsrfInterceptor> csrfInterceptorProvider;

  private final Provider<GatewayWebSocketManager> gatewayManagerProvider;

  public AuthRepository_Factory(Provider<FluxerApiService> apiServiceProvider,
      Provider<SecureCookieStorage> cookieStorageProvider,
      Provider<CsrfInterceptor> csrfInterceptorProvider,
      Provider<GatewayWebSocketManager> gatewayManagerProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.cookieStorageProvider = cookieStorageProvider;
    this.csrfInterceptorProvider = csrfInterceptorProvider;
    this.gatewayManagerProvider = gatewayManagerProvider;
  }

  @Override
  public AuthRepository get() {
    return newInstance(apiServiceProvider.get(), cookieStorageProvider.get(), csrfInterceptorProvider.get(), gatewayManagerProvider.get());
  }

  public static AuthRepository_Factory create(Provider<FluxerApiService> apiServiceProvider,
      Provider<SecureCookieStorage> cookieStorageProvider,
      Provider<CsrfInterceptor> csrfInterceptorProvider,
      Provider<GatewayWebSocketManager> gatewayManagerProvider) {
    return new AuthRepository_Factory(apiServiceProvider, cookieStorageProvider, csrfInterceptorProvider, gatewayManagerProvider);
  }

  public static AuthRepository newInstance(FluxerApiService apiService,
      SecureCookieStorage cookieStorage, CsrfInterceptor csrfInterceptor,
      GatewayWebSocketManager gatewayManager) {
    return new AuthRepository(apiService, cookieStorage, csrfInterceptor, gatewayManager);
  }
}
