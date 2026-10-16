package com.smsmurilo.telecom

import android.app.Application
import com.smsmurilo.telecom.data.DataStoreSettingsRepository
import com.smsmurilo.telecom.data.FakeTelecomRepository
import com.smsmurilo.telecom.domain.SettingsRepository
import com.smsmurilo.telecom.domain.TelecomRepository
import com.smsmurilo.telecom.mvi.HomeViewModel
import com.smsmurilo.telecom.work.UsageAlertWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single<TelecomRepository> { FakeTelecomRepository() }
    single { DataStoreSettingsRepository(androidContext()) } bind SettingsRepository::class
    viewModel { HomeViewModel(get(), get()) }
}

class TelecomApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TelecomApp)
            modules(appModule)
        }
        UsageAlertWorker.schedule(this)
    }
}
