package com.fluxer.client.data.repository;

import com.fluxer.client.data.local.AuthTokenStorage;
import com.fluxer.client.data.local.InstanceConfigStore;
import com.fluxer.client.data.local.SecureCookieStorage;
import com.fluxer.client.data.remote.AuthAuthenticator;
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

  private final Provider<InstanceConfigStore> instanceConfigStoreProvider;

  private final Provider<CsrfInterceptor> csrfInterceptorProvider;

  private final Provider<AuthAuthenticator> authenticatorProvider;

  private final Provider<GatewayWebSocketManager> gatewayManagerProvider;

  private final Provider<AuthTokenStorage> authTokenStorageProvider;

  public AuthRepository_Factory(Provider<FluxerApiService> apiServiceProvider,
      Provider<SecureCookieStorage> cookieStorageProvider,
      Provider<InstanceConfigStore> instanceConfigStoreProvider,
      Provider<CsrfInterceptor> csrfInterceptorProvider,
      Provider<AuthAuthenticator> authenticatorProvider,
      Provider<GatewayWebSocketManager> gatewayManagerProvider,
      Provider<AuthTokenStorage> authTokenStorageProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.cookieStorageProvider = cookieStorageProvider;
    this.instanceConfigStoreProvider = instanceConfigStoreProvider;
    this.csrfInterceptorProvider = csrfInterceptorProvider;
    this.authenticatorProvider = authenticatorProvider;
    this.gatewayManagerProvider = gatewayManagerProvider;
    this.authTokenStorageProvider = authTokenStorageProvider;
  }

  @Override
  public AuthRepository get() {
    return newInstance(apiServiceProvider.get(), cookieStorageProvider.get(), instanceConfigStoreProvider.get(), csrfInterceptorProvider.get(), authenticatorProvider.get(), gatewayManagerProvider.get(), authTokenStorageProvider.get());
  }

  public static AuthRepository_Factory create(Provider<FluxerApiService> apiServiceProvider,
      Provider<SecureCookieStorage> cookieStorageProvider,
      Provider<InstanceConfigStore> instanceConfigStoreProvider,
      Provider<CsrfInterceptor> csrfInterceptorProvider,
      Provider<AuthAuthenticator> authenticatorProvider,
      Provider<GatewayWebSocketManager> gatewayManagerProvider,
      Provider<AuthTokenStorage> authTokenStorageProvider) {
    return new AuthRepository_Factory(apiServiceProvider, cookieStorageProvider, instanceConfigStoreProvider, csrfInterceptorProvider, authenticatorProvider, gatewayManagerProvider, authTokenStorageProvider);
  }

  public static AuthRepository newInstance(FluxerApiService apiService,
      SecureCookieStorage cookieStorage, InstanceConfigStore instanceConfigStore,
      CsrfInterceptor csrfInterceptor, AuthAuthenticator authenticator,
      GatewayWebSocketManager gatewayManager, AuthTokenStorage authTokenStorage) {
    return new AuthRepository(apiService, cookieStorage, instanceConfigStore, csrfInterceptor, authenticator, gatewayManager, authTokenStorage);
  }
}
