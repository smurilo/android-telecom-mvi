package com.smsmurilo.telecom.domain

/** Decides whether to notify, avoiding spam: one alert per threshold crossed per billing cycle. */
object UsageAlertPolicy {

    data class Decision(val notify: Boolean, val message: String?)

    fun evaluate(usage: PlanUsage, thresholdPercent: Int, lastAlertedPercentInCycle: Int?): Decision {
        val pct = usage.usedPercent
        return when {
            pct >= 100 && (lastAlertedPercentInCycle ?: 0) < 100 ->
                Decision(true, "Sua franquia de dados acabou. Contrate um pacote extra para continuar navegando.")
            pct in thresholdPercent until 100 && (lastAlertedPercentInCycle ?: 0) < thresholdPercent ->
                Decision(true, "Você já usou %d%% da sua internet. Restam %.1f GB.".format(pct, usage.dataLeftMb / 1024.0))
            else -> Decision(false, null)
        }
    }
}
