package com.fluxer.client.data.repository;

import android.content.Context;
import com.fluxer.client.data.local.dao.DmChannelDao;
import com.fluxer.client.data.remote.AvatarApiService;
import com.fluxer.client.data.remote.FluxerApiService;
import com.fluxer.client.data.remote.GatewayWebSocketManager;
import com.fluxer.client.data.remote.GuildMembersApiService;
import com.fluxer.client.data.remote.InviteApiService;
import com.fluxer.client.data.remote.PinApiService;
import com.fluxer.client.data.remote.UploadApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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

  private final Provider<UploadApiService> uploadApiServiceProvider;

  private final Provider<PinApiService> pinApiServiceProvider;

  private final Provider<GuildMembersApiService> guildMembersApiServiceProvider;

  private final Provider<InviteApiService> inviteApiServiceProvider;

  private final Provider<AvatarApiService> avatarApiServiceProvider;

  private final Provider<GatewayWebSocketManager> gatewayManagerProvider;

  private final Provider<DmChannelDao> dmChannelDaoProvider;

  private final Provider<Context> contextProvider;

  public ChatRepository_Factory(Provider<FluxerApiService> apiServiceProvider,
      Provider<UploadApiService> uploadApiServiceProvider,
      Provider<PinApiService> pinApiServiceProvider,
      Provider<GuildMembersApiService> guildMembersApiServiceProvider,
      Provider<InviteApiService> inviteApiServiceProvider,
      Provider<AvatarApiService> avatarApiServiceProvider,
      Provider<GatewayWebSocketManager> gatewayManagerProvider,
      Provider<DmChannelDao> dmChannelDaoProvider, Provider<Context> contextProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.uploadApiServiceProvider = uploadApiServiceProvider;
    this.pinApiServiceProvider = pinApiServiceProvider;
    this.guildMembersApiServiceProvider = guildMembersApiServiceProvider;
    this.inviteApiServiceProvider = inviteApiServiceProvider;
    this.avatarApiServiceProvider = avatarApiServiceProvider;
    this.gatewayManagerProvider = gatewayManagerProvider;
    this.dmChannelDaoProvider = dmChannelDaoProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public ChatRepository get() {
    return newInstance(apiServiceProvider.get(), uploadApiServiceProvider.get(), pinApiServiceProvider.get(), guildMembersApiServiceProvider.get(), inviteApiServiceProvider.get(), avatarApiServiceProvider.get(), gatewayManagerProvider.get(), dmChannelDaoProvider.get(), contextProvider.get());
  }

  public static ChatRepository_Factory create(Provider<FluxerApiService> apiServiceProvider,
      Provider<UploadApiService> uploadApiServiceProvider,
      Provider<PinApiService> pinApiServiceProvider,
      Provider<GuildMembersApiService> guildMembersApiServiceProvider,
      Provider<InviteApiService> inviteApiServiceProvider,
      Provider<AvatarApiService> avatarApiServiceProvider,
      Provider<GatewayWebSocketManager> gatewayManagerProvider,
      Provider<DmChannelDao> dmChannelDaoProvider, Provider<Context> contextProvider) {
    return new ChatRepository_Factory(apiServiceProvider, uploadApiServiceProvider, pinApiServiceProvider, guildMembersApiServiceProvider, inviteApiServiceProvider, avatarApiServiceProvider, gatewayManagerProvider, dmChannelDaoProvider, contextProvider);
  }

  public static ChatRepository newInstance(FluxerApiService apiService,
      UploadApiService uploadApiService, PinApiService pinApiService,
      GuildMembersApiService guildMembersApiService, InviteApiService inviteApiService,
      AvatarApiService avatarApiService, GatewayWebSocketManager gatewayManager,
      DmChannelDao dmChannelDao, Context context) {
    return new ChatRepository(apiService, uploadApiService, pinApiService, guildMembersApiService, inviteApiService, avatarApiService, gatewayManager, dmChannelDao, context);
  }
}
