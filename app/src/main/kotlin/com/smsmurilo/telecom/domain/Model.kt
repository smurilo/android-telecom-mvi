package com.smsmurilo.telecom.domain

import java.math.BigDecimal
import java.time.LocalDate

data class PlanUsage(
    val planName: String,
    val msisdn: String,
    val dataTotalMb: Long,
    val dataUsedMb: Long,
    val minutesUnlimited: Boolean,
    val cycleEnd: LocalDate,
) {
    val dataLeftMb get() = (dataTotalMb - dataUsedMb).coerceAtLeast(0)
    val usedPercent get() = if (dataTotalMb == 0L) 0 else ((dataUsedMb * 100) / dataTotalMb).toInt().coerceIn(0, 100)
}

enum class InvoiceStatus { OPEN, PAID, OVERDUE }

data class Invoice(val id: String, val reference: String, val amount: BigDecimal, val dueDate: LocalDate, val status: InvoiceStatus, val barcode: String)

data class DataPackage(val id: String, val name: String, val extraMb: Long, val price: BigDecimal)

interface TelecomRepository {
    suspend fun usage(): PlanUsage
    suspend fun invoices(): List<Invoice>
    suspend fun packages(): List<DataPackage>
    suspend fun buyPackage(packageId: String): PlanUsage
}

interface SettingsRepository {
    val alertThreshold: kotlinx.coroutines.flow.Flow<Int>
    suspend fun setAlertThreshold(percent: Int)
}
