package com.smsmurilo.telecom.data

import com.smsmurilo.telecom.domain.DataPackage
import com.smsmurilo.telecom.domain.Invoice
import com.smsmurilo.telecom.domain.InvoiceStatus
import com.smsmurilo.telecom.domain.PlanUsage
import com.smsmurilo.telecom.domain.TelecomRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.time.LocalDate

/** In-memory backend simulation (latency included) so the showcase runs without a server. */
class FakeTelecomRepository(private val latencyMs: Long = 700) : TelecomRepository {

    private val mutex = Mutex()
    private var usage = PlanUsage("Controle 25GB", "(82) 99999-0000", 25 * 1024, 21 * 1024, true, LocalDate.now().plusDays(9))

    private val packages = listOf(
        DataPackage("p1", "+1 GB por 7 dias", 1024, BigDecimal("9.99")),
        DataPackage("p2", "+5 GB por 30 dias", 5 * 1024, BigDecimal("29.99")),
        DataPackage("p3", "Redes sociais ilimitadas", 0, BigDecimal("14.99")),
    )

    override suspend fun usage(): PlanUsage { delay(latencyMs); return mutex.withLock { usage } }

    override suspend fun invoices(): List<Invoice> {
        delay(latencyMs)
        val today = LocalDate.now()
        return listOf(
            Invoice("i3", today.month.name, BigDecimal("59.90"), today.plusDays(5), InvoiceStatus.OPEN, "84670000001-5 59900138001-1"),
            Invoice("i2", today.minusMonths(1).month.name, BigDecimal("59.90"), today.minusDays(25), InvoiceStatus.PAID, ""),
            Invoice("i1", today.minusMonths(2).month.name, BigDecimal("64.40"), today.minusDays(55), InvoiceStatus.PAID, ""),
        )
    }

    override suspend fun packages(): List<DataPackage> { delay(latencyMs / 2); return packages }

    override suspend fun buyPackage(packageId: String): PlanUsage {
        delay(latencyMs)
        val pkg = packages.first { it.id == packageId }
        return mutex.withLock { usage.copy(dataTotalMb = usage.dataTotalMb + pkg.extraMb).also { usage = it } }
    }
}
