package com.fluxer.client.ui.viewmodel;

import com.fluxer.client.data.local.InstanceConfigStore;
import com.fluxer.client.data.repository.AuthRepository;
import com.fluxer.client.data.repository.ChatRepository;
import com.fluxer.client.data.repository.GuildManagementRepository;
import com.fluxer.client.data.repository.HomeStateRepository;
import com.fluxer.client.data.repository.ProfileRepository;
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<HomeStateRepository> homeStateRepositoryProvider;

  private final Provider<GuildManagementRepository> guildManagementRepositoryProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  private final Provider<InstanceConfigStore> instanceConfigStoreProvider;

  public ChatViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<HomeStateRepository> homeStateRepositoryProvider,
      Provider<GuildManagementRepository> guildManagementRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<InstanceConfigStore> instanceConfigStoreProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.homeStateRepositoryProvider = homeStateRepositoryProvider;
    this.guildManagementRepositoryProvider = guildManagementRepositoryProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
    this.instanceConfigStoreProvider = instanceConfigStoreProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(chatRepositoryProvider.get(), authRepositoryProvider.get(), homeStateRepositoryProvider.get(), guildManagementRepositoryProvider.get(), profileRepositoryProvider.get(), instanceConfigStoreProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<ChatRepository> chatRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<HomeStateRepository> homeStateRepositoryProvider,
      Provider<GuildManagementRepository> guildManagementRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<InstanceConfigStore> instanceConfigStoreProvider) {
    return new ChatViewModel_Factory(chatRepositoryProvider, authRepositoryProvider, homeStateRepositoryProvider, guildManagementRepositoryProvider, profileRepositoryProvider, instanceConfigStoreProvider);
  }

  public static ChatViewModel newInstance(ChatRepository chatRepository,
      AuthRepository authRepository, HomeStateRepository homeStateRepository,
      GuildManagementRepository guildManagementRepository, ProfileRepository profileRepository,
      InstanceConfigStore instanceConfigStore) {
    return new ChatViewModel(chatRepository, authRepository, homeStateRepository, guildManagementRepository, profileRepository, instanceConfigStore);
  }
}
