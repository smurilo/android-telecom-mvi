package com.smsmurilo.telecom

import com.smsmurilo.telecom.domain.PlanUsage
import com.smsmurilo.telecom.mvi.HomeResult
import com.smsmurilo.telecom.mvi.HomeState
import com.smsmurilo.telecom.mvi.reduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HomeReducerTest {

    private val usage = PlanUsage("Plano", "1", 1000, 500, true, LocalDate.of(2026, 3, 1))

    @Test
    fun `loaded clears loading and fills data`() {
        val state = reduce(HomeState(), HomeResult.Loaded(usage, emptyList(), emptyList()))
        assertFalse(state.loading)
        assertEquals(usage, state.usage)
    }

    @Test
    fun `buying then bought updates usage and clears in-flight purchase`() {
        val buying = reduce(HomeState(usage = usage), HomeResult.Buying("p1"))
        assertEquals("p1", buying.buyingPackageId)
        val bought = reduce(buying, HomeResult.Bought(usage.copy(dataTotalMb = 2000)))
        assertNull(bought.buyingPackageId)
        assertEquals(2000L, bought.usage?.dataTotalMb)
    }

    @Test
    fun `load failure keeps previous data`() {
        val state = reduce(HomeState(usage = usage, loading = true), HomeResult.LoadFailed("erro"))
        assertEquals(usage, state.usage)
        assertEquals("erro", state.error)
    }
}
