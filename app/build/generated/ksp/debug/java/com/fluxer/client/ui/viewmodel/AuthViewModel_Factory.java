package com.fluxer.client.ui.viewmodel;

import com.fluxer.client.data.local.InstanceConfigStore;
import com.fluxer.client.data.remote.WebAuthnService;
import com.fluxer.client.data.repository.AuthRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AuthViewModel_Factory implements Factory<AuthViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<InstanceConfigStore> instanceConfigStoreProvider;

  private final Provider<WebAuthnService> webAuthnServiceProvider;

  public AuthViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<InstanceConfigStore> instanceConfigStoreProvider,
      Provider<WebAuthnService> webAuthnServiceProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.instanceConfigStoreProvider = instanceConfigStoreProvider;
    this.webAuthnServiceProvider = webAuthnServiceProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(authRepositoryProvider.get(), instanceConfigStoreProvider.get(), webAuthnServiceProvider.get());
  }

  public static AuthViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<InstanceConfigStore> instanceConfigStoreProvider,
      Provider<WebAuthnService> webAuthnServiceProvider) {
    return new AuthViewModel_Factory(authRepositoryProvider, instanceConfigStoreProvider, webAuthnServiceProvider);
  }

  public static AuthViewModel newInstance(AuthRepository authRepository,
      InstanceConfigStore instanceConfigStore, WebAuthnService webAuthnService) {
    return new AuthViewModel(authRepository, instanceConfigStore, webAuthnService);
  }
}
