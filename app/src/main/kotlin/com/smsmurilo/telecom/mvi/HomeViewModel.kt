package com.smsmurilo.telecom.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsmurilo.telecom.domain.SettingsRepository
import com.smsmurilo.telecom.domain.TelecomRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: TelecomRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Unlimited channel: intents sent before the collector starts are queued, never dropped. */
    private val intents = Channel<HomeIntent>(Channel.UNLIMITED)

    init {
        viewModelScope.launch { intents.consumeAsFlow().collect(::handle) }
        viewModelScope.launch { settings.alertThreshold.collect { emit(HomeResult.ThresholdChanged(it)) } }
        process(HomeIntent.Load)
    }

    fun process(intent: HomeIntent) {
        intents.trySend(intent)
    }

    private fun emit(result: HomeResult) {
        _state.value = reduce(_state.value, result)
    }

    private suspend fun handle(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> viewModelScope.launch { load() }
            is HomeIntent.BuyPackage -> {
                if (_state.value.buyingPackageId != null) return // ignore double taps while a purchase is in flight
                // Mark as in-flight synchronously, before the coroutine starts, so the guard above holds.
                emit(HomeResult.Buying(intent.packageId))
                viewModelScope.launch { buy(intent.packageId) }
            }
            is HomeIntent.CopyBarcode -> _effects.send(HomeEffect.CopyToClipboard(intent.invoice.barcode))
            is HomeIntent.ChangeThreshold -> settings.setAlertThreshold(intent.percent)
        }
    }

    private suspend fun load() {
        emit(HomeResult.Loading)
        runCatching {
            coroutineScope {
                val usage = async { repository.usage() }
                val invoices = async { repository.invoices() }
                val packages = async { repository.packages() }
                HomeResult.Loaded(usage.await(), invoices.await(), packages.await())
            }
        }.onSuccess(::emit).onFailure { emit(HomeResult.LoadFailed("Não foi possível carregar seus dados.")) }
    }

    private suspend fun buy(packageId: String) {
        runCatching { repository.buyPackage(packageId) }
            .onSuccess {
                emit(HomeResult.Bought(it))
                _effects.send(HomeEffect.Toast("Pacote contratado com sucesso!"))
            }
            .onFailure {
                emit(HomeResult.BuyFailed(it.message ?: ""))
                _effects.send(HomeEffect.Toast("Falha na contratação. Nada foi cobrado."))
            }
    }
}
