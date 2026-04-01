package com.fluxer.client.di;

import com.fluxer.client.data.local.AppDatabase;
import com.fluxer.client.data.local.dao.GuildDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideGuildDaoFactory implements Factory<GuildDao> {
  private final Provider<AppDatabase> appDatabaseProvider;

  public DatabaseModule_ProvideGuildDaoFactory(Provider<AppDatabase> appDatabaseProvider) {
    this.appDatabaseProvider = appDatabaseProvider;
  }

  @Override
  public GuildDao get() {
    return provideGuildDao(appDatabaseProvider.get());
  }

  public static DatabaseModule_ProvideGuildDaoFactory create(
      Provider<AppDatabase> appDatabaseProvider) {
    return new DatabaseModule_ProvideGuildDaoFactory(appDatabaseProvider);
  }

  public static GuildDao provideGuildDao(AppDatabase appDatabase) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideGuildDao(appDatabase));
  }
}
