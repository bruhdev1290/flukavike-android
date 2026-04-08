package com.fluxer.client.ui.viewmodel;

import com.fluxer.client.data.local.InstanceConfigStore;
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

  public AuthViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<InstanceConfigStore> instanceConfigStoreProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.instanceConfigStoreProvider = instanceConfigStoreProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(authRepositoryProvider.get(), instanceConfigStoreProvider.get());
  }

  public static AuthViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<InstanceConfigStore> instanceConfigStoreProvider) {
    return new AuthViewModel_Factory(authRepositoryProvider, instanceConfigStoreProvider);
  }

  public static AuthViewModel newInstance(AuthRepository authRepository,
      InstanceConfigStore instanceConfigStore) {
    return new AuthViewModel(authRepository, instanceConfigStore);
  }
}
