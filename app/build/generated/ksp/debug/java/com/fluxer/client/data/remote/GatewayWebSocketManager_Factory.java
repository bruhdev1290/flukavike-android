package com.fluxer.client.data.remote;

import com.fluxer.client.data.local.AuthTokenStorage;
import com.fluxer.client.data.local.InstanceConfigStore;
import com.fluxer.client.data.local.SecureCookieStorage;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;

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
public final class GatewayWebSocketManager_Factory implements Factory<GatewayWebSocketManager> {
  private final Provider<SecureCookieStorage> cookieStorageProvider;

  private final Provider<AuthTokenStorage> authTokenStorageProvider;

  private final Provider<Json> jsonProvider;

  private final Provider<InstanceConfigStore> instanceConfigStoreProvider;

  public GatewayWebSocketManager_Factory(Provider<SecureCookieStorage> cookieStorageProvider,
      Provider<AuthTokenStorage> authTokenStorageProvider, Provider<Json> jsonProvider,
      Provider<InstanceConfigStore> instanceConfigStoreProvider) {
    this.cookieStorageProvider = cookieStorageProvider;
    this.authTokenStorageProvider = authTokenStorageProvider;
    this.jsonProvider = jsonProvider;
    this.instanceConfigStoreProvider = instanceConfigStoreProvider;
  }

  @Override
  public GatewayWebSocketManager get() {
    return newInstance(cookieStorageProvider.get(), authTokenStorageProvider.get(), jsonProvider.get(), instanceConfigStoreProvider.get());
  }

  public static GatewayWebSocketManager_Factory create(
      Provider<SecureCookieStorage> cookieStorageProvider,
      Provider<AuthTokenStorage> authTokenStorageProvider, Provider<Json> jsonProvider,
      Provider<InstanceConfigStore> instanceConfigStoreProvider) {
    return new GatewayWebSocketManager_Factory(cookieStorageProvider, authTokenStorageProvider, jsonProvider, instanceConfigStoreProvider);
  }

  public static GatewayWebSocketManager newInstance(SecureCookieStorage cookieStorage,
      AuthTokenStorage authTokenStorage, Json json, InstanceConfigStore instanceConfigStore) {
    return new GatewayWebSocketManager(cookieStorage, authTokenStorage, json, instanceConfigStore);
  }
}
