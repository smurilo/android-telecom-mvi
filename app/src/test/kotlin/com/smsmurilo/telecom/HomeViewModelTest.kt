package com.smsmurilo.telecom

import app.cash.turbine.test
import com.smsmurilo.telecom.data.FakeTelecomRepository
import com.smsmurilo.telecom.domain.SettingsRepository
import com.smsmurilo.telecom.mvi.HomeEffect
import com.smsmurilo.telecom.mvi.HomeIntent
import com.smsmurilo.telecom.mvi.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeSettings : SettingsRepository {
        override val alertThreshold = MutableStateFlow(80)
        override suspend fun setAlertThreshold(percent: Int) { alertThreshold.value = percent }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads usage, invoices and packages in parallel`() = runTest(dispatcher) {
        val vm = HomeViewModel(FakeTelecomRepository(latencyMs = 100), FakeSettings())
        advanceUntilIdle()
        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(3, state.invoices.size)
        assertEquals(3, state.packages.size)
        // three calls of 100ms/100ms/50ms ran concurrently, not sequentially
        assertEquals(100L, testScheduler.currentTime)
    }

    @Test
    fun `buying a package increases allowance and emits a toast`() = runTest(dispatcher) {
        val vm = HomeViewModel(FakeTelecomRepository(latencyMs = 10), FakeSettings())
        advanceUntilIdle()
        val before = vm.state.value.usage!!.dataTotalMb
        vm.effects.test {
            vm.process(HomeIntent.BuyPackage("p2"))
            vm.process(HomeIntent.BuyPackage("p2")) // double tap is ignored
            advanceUntilIdle()
            assertEquals(HomeEffect.Toast("Pacote contratado com sucesso!"), awaitItem())
            expectNoEvents()
        }
        assertEquals(before + 5 * 1024, vm.state.value.usage!!.dataTotalMb)
    }

    @Test
    fun `threshold changes flow from settings into state`() = runTest(dispatcher) {
        val settings = FakeSettings()
        val vm = HomeViewModel(FakeTelecomRepository(latencyMs = 1), settings)
        vm.process(HomeIntent.ChangeThreshold(90))
        advanceUntilIdle()
        assertEquals(90, vm.state.value.alertThreshold)
    }
}
