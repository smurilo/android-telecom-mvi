package com.smsmurilo.telecom.mvi

import com.smsmurilo.telecom.domain.DataPackage
import com.smsmurilo.telecom.domain.Invoice
import com.smsmurilo.telecom.domain.PlanUsage

/** MVI contract: the screen can only send Intents and render State; one-off events go through Effect. */
data class HomeState(
    val loading: Boolean = true,
    val usage: PlanUsage? = null,
    val invoices: List<Invoice> = emptyList(),
    val packages: List<DataPackage> = emptyList(),
    val buyingPackageId: String? = null,
    val alertThreshold: Int = 80,
    val error: String? = null,
)

sealed interface HomeIntent {
    data object Load : HomeIntent
    data class BuyPackage(val packageId: String) : HomeIntent
    data class CopyBarcode(val invoice: Invoice) : HomeIntent
    data class ChangeThreshold(val percent: Int) : HomeIntent
}

/** Results of side effects, fed back into the reducer. */
sealed interface HomeResult {
    data object Loading : HomeResult
    data class Loaded(val usage: PlanUsage, val invoices: List<Invoice>, val packages: List<DataPackage>) : HomeResult
    data class LoadFailed(val message: String) : HomeResult
    data class Buying(val packageId: String) : HomeResult
    data class Bought(val usage: PlanUsage) : HomeResult
    data class BuyFailed(val message: String) : HomeResult
    data class ThresholdChanged(val percent: Int) : HomeResult
}

sealed interface HomeEffect {
    data class Toast(val message: String) : HomeEffect
    data class CopyToClipboard(val text: String) : HomeEffect
}

/** Pure function: trivially unit-testable and the only place state changes. */
fun reduce(state: HomeState, result: HomeResult): HomeState = when (result) {
    HomeResult.Loading -> state.copy(loading = true, error = null)
    is HomeResult.Loaded -> state.copy(loading = false, usage = result.usage, invoices = result.invoices, packages = result.packages)
    is HomeResult.LoadFailed -> state.copy(loading = false, error = result.message)
    is HomeResult.Buying -> state.copy(buyingPackageId = result.packageId)
    is HomeResult.Bought -> state.copy(buyingPackageId = null, usage = result.usage)
    is HomeResult.BuyFailed -> state.copy(buyingPackageId = null)
    is HomeResult.ThresholdChanged -> state.copy(alertThreshold = result.percent)
}
