package com.fluxer.client.di;

import com.fluxer.client.data.local.AppDatabase;
import com.fluxer.client.data.local.dao.PendingMessageDao;
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
public final class DatabaseModule_ProvidePendingMessageDaoFactory implements Factory<PendingMessageDao> {
  private final Provider<AppDatabase> appDatabaseProvider;

  public DatabaseModule_ProvidePendingMessageDaoFactory(Provider<AppDatabase> appDatabaseProvider) {
    this.appDatabaseProvider = appDatabaseProvider;
  }

  @Override
  public PendingMessageDao get() {
    return providePendingMessageDao(appDatabaseProvider.get());
  }

  public static DatabaseModule_ProvidePendingMessageDaoFactory create(
      Provider<AppDatabase> appDatabaseProvider) {
    return new DatabaseModule_ProvidePendingMessageDaoFactory(appDatabaseProvider);
  }

  public static PendingMessageDao providePendingMessageDao(AppDatabase appDatabase) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePendingMessageDao(appDatabase));
  }
}
