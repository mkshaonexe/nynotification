package com.quietinbox.di

import com.quietinbox.core.apps.DefaultInstalledAppsProvider
import com.quietinbox.core.apps.InstalledAppsProvider
import com.quietinbox.core.firewall.DefaultFirewallEngine
import com.quietinbox.core.firewall.DefaultScheduleEvaluator
import com.quietinbox.core.firewall.FirewallEngine
import com.quietinbox.core.firewall.ScheduleEvaluator
import com.quietinbox.core.signal.DefaultSignalClassifier
import com.quietinbox.core.signal.SignalClassifier
import com.quietinbox.core.time.Clock
import com.quietinbox.core.time.DefaultClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindClock(clock: DefaultClock): Clock

    @Binds
    @Singleton
    abstract fun bindSignalClassifier(classifier: DefaultSignalClassifier): SignalClassifier

    @Binds
    @Singleton
    abstract fun bindFirewallEngine(engine: DefaultFirewallEngine): FirewallEngine

    @Binds
    @Singleton
    abstract fun bindScheduleEvaluator(evaluator: DefaultScheduleEvaluator): ScheduleEvaluator

    @Binds
    @Singleton
    abstract fun bindInstalledAppsProvider(provider: DefaultInstalledAppsProvider): InstalledAppsProvider
}
