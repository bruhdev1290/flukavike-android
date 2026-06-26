package com.fluxer.client.data.repository;

import com.fluxer.client.data.local.dao.DmChannelDao;
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
public final class ChatRepository_Factory implements Factory<ChatRepository> {
  private final Provider<FluxerApiService> apiServiceProvider;

  private final Provider<GatewayWebSocketManager> gatewayManagerProvider;

  private final Provider<DmChannelDao> dmChannelDaoProvider;

  public ChatRepository_Factory(Provider<FluxerApiService> apiServiceProvider,
      Provider<GatewayWebSocketManager> gatewayManagerProvider,
      Provider<DmChannelDao> dmChannelDaoProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.gatewayManagerProvider = gatewayManagerProvider;
    this.dmChannelDaoProvider = dmChannelDaoProvider;
  }

  @Override
  public ChatRepository get() {
    return newInstance(apiServiceProvider.get(), gatewayManagerProvider.get(), dmChannelDaoProvider.get());
  }

  public static ChatRepository_Factory create(Provider<FluxerApiService> apiServiceProvider,
      Provider<GatewayWebSocketManager> gatewayManagerProvider,
      Provider<DmChannelDao> dmChannelDaoProvider) {
    return new ChatRepository_Factory(apiServiceProvider, gatewayManagerProvider, dmChannelDaoProvider);
  }

  public static ChatRepository newInstance(FluxerApiService apiService,
      GatewayWebSocketManager gatewayManager, DmChannelDao dmChannelDao) {
    return new ChatRepository(apiService, gatewayManager, dmChannelDao);
  }
}
