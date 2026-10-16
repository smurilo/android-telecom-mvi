package com.smsmurilo.telecom

import com.smsmurilo.telecom.domain.PlanUsage
import com.smsmurilo.telecom.domain.UsageAlertPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UsageAlertPolicyTest {

    private fun usage(usedPercent: Int) = PlanUsage("P", "1", 1000, usedPercent * 10L, false, LocalDate.now())

    @Test
    fun `notifies once when crossing the threshold`() {
        assertTrue(UsageAlertPolicy.evaluate(usage(82), 80, null).notify)
        assertFalse(UsageAlertPolicy.evaluate(usage(85), 80, 82).notify)
    }

    @Test
    fun `notifies again when allowance is exhausted`() {
        val decision = UsageAlertPolicy.evaluate(usage(100), 80, 85)
        assertTrue(decision.notify)
        assertTrue(decision.message!!.contains("acabou"))
        assertFalse(UsageAlertPolicy.evaluate(usage(100), 80, 100).notify)
    }

    @Test
    fun `stays quiet below threshold`() {
        assertFalse(UsageAlertPolicy.evaluate(usage(60), 80, null).notify)
    }
}
