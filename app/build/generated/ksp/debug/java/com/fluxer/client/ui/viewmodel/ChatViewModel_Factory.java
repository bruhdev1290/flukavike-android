package com.fluxer.client.ui.viewmodel;

import com.fluxer.client.data.repository.AuthRepository;
import com.fluxer.client.data.repository.ChatRepository;
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

  public ChatViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(chatRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<ChatRepository> chatRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new ChatViewModel_Factory(chatRepositoryProvider, authRepositoryProvider);
  }

  public static ChatViewModel newInstance(ChatRepository chatRepository,
      AuthRepository authRepository) {
    return new ChatViewModel(chatRepository, authRepository);
  }
}
