package com.quietinbox.di

import com.quietinbox.data.db.QuietDatabase
import com.quietinbox.feature.inbox.data.InboxDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Phase 4 (Inbox) dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object P4Module {

    @Provides
    @Singleton
    fun provideInboxDao(db: QuietDatabase): InboxDao = db.inboxDao()
}
