package com.fluxer.client.service

import com.fluxer.client.data.repository.NotificationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FluxerNotificationEntryPoint {
    fun notificationRepository(): NotificationRepository
    fun navigationRepository(): com.fluxer.client.data.repository.NavigationRepository
}
