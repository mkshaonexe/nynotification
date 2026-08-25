package com.quietinbox.di

import com.quietinbox.core.health.DefaultListenerHealth
import com.quietinbox.core.health.ListenerHealth
import com.quietinbox.service.ingest.DefaultIngestPipeline
import com.quietinbox.service.ingest.IngestPipeline
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class P1Module {

    @Binds
    @Singleton
    abstract fun bindIngestPipeline(pipeline: DefaultIngestPipeline): IngestPipeline

    @Binds
    @Singleton
    abstract fun bindListenerHealth(health: DefaultListenerHealth): ListenerHealth
}
