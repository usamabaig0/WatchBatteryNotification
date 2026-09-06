package com.watchbattery.notification.core.physics

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/**
 * 20 piecewise 5% battery blocks modeling non-linear CC-CV (Constant Current - Constant Voltage)
 * charging characteristics of Lithium-Ion battery cells.
 */
enum class ChargeBlock(
    val startPercent: Int,
    val endPercent: Int,
    val defaultMinutesPerBlock: Double
) {
    B_0_5(0, 5, 2.5),
    B_5_10(5, 10, 2.5),
    B_10_15(10, 15, 2.5),
    B_15_20(15, 20, 2.5),
    B_20_25(20, 25, 2.5),
    B_25_30(25, 30, 2.6),
    B_30_35(30, 35, 2.6),
    B_35_40(35, 40, 2.7),
    B_40_45(40, 45, 2.7),
    B_45_50(45, 50, 2.8),
    B_50_55(50, 55, 2.9),
    B_55_60(55, 60, 3.0),
    B_60_65(60, 65, 3.2),
    B_65_70(65, 70, 3.5),
    B_70_75(70, 75, 4.0),
    B_75_80(75, 80, 4.5),
    B_80_85(80, 85, 5.5),   // Constant Voltage (CV) taper begins
    B_85_90(85, 90, 7.0),   // Current decreases exponentially
    B_90_95(90, 95, 9.5),   // Saturation phase
    B_95_100(95, 100, 12.0); // Trickle charging

    companion object {
        fun forPercent(percent: Int): ChargeBlock {
            val clamped = percent.coerceIn(0, 99)
            return values().firstOrNull { clamped in it.startPercent until it.endPercent } ?: B_95_100
        }
    }
}

@Serializable
data class BatterySample(
    val timestamp: Long,
    val batteryLevel: Int,
    val isCharging: Boolean
)

@Serializable
data class PredictiveModelData(
    val learnedBlockMinutes: Map<String, Double> = emptyMap(),
    val averageChargeVelocityPercentPerMin: Double = 0.0,
    val averageDischargePercentPerHour: Double = 0.0,
    val totalSessionsAnalyzed: Int = 0,
    val confidenceScore: Double = 0.0,
    val anomalyBudgetRemaining: Int = 5,
    val daytimeDischargePerHour: Double = 0.0,
    val nightDischargePerHour: Double = 0.0
) {
    val hasRecordedData: Boolean
        get() = totalSessionsAnalyzed > 0
}

object ChargePredictivePhysics {

    private const val EMA_WEIGHT = 0.7 // 70% historical, 30% new reading

    /**
     * Calculates estimated minutes remaining until target percentage is reached.
     */
    fun calculateMinutesToTarget(
        currentLevel: Int,
        targetLevel: Int,
        model: PredictiveModelData
    ): Int {
        if (currentLevel < 0 || currentLevel >= targetLevel) return 0
        var totalMinutes = 0.0

        for (block in ChargeBlock.values()) {
            if (block.endPercent <= currentLevel) continue
            if (block.startPercent >= targetLevel) break

            val blockStart = max(currentLevel, block.startPercent)
            val blockEnd = min(targetLevel, block.endPercent)
            val percentSpan = (blockEnd - blockStart).coerceAtLeast(0)

            val learnedDuration = model.learnedBlockMinutes[block.name] ?: block.defaultMinutesPerBlock
            val minutesPerPercent = learnedDuration / 5.0

            totalMinutes += percentSpan * minutesPerPercent
        }

        return max(1, totalMinutes.toInt())
    }

    /**
     * Calculates estimated hours and minutes until battery drops to 0%.
     */
    fun calculateDischargeTimeRemaining(
        currentLevel: Int,
        isNightTime: Boolean,
        model: PredictiveModelData
    ): Pair<Int, Int> {
        if (currentLevel <= 0) return Pair(0, 0)
        val ratePerHour = when {
            isNightTime && model.nightDischargePerHour > 0 -> model.nightDischargePerHour
            !isNightTime && model.daytimeDischargePerHour > 0 -> model.daytimeDischargePerHour
            isNightTime -> 1.5 // default night estimate
            else -> 3.5        // default daytime estimate
        }

        val totalHours = currentLevel / ratePerHour.coerceAtLeast(0.5)
        val hours = totalHours.toInt()
        val minutes = ((totalHours - hours) * 60).toInt()
        return Pair(hours, minutes)
    }

    /**
     * Updates learned block speed using Exponential Moving Average.
     */
    fun updateBlockWithSample(
        block: ChargeBlock,
        measuredMinutes: Double,
        currentModel: PredictiveModelData
    ): PredictiveModelData {
        if (measuredMinutes < 0.2 || measuredMinutes > 45.0) {
            return currentModel.copy(
                anomalyBudgetRemaining = (currentModel.anomalyBudgetRemaining - 1).coerceAtLeast(0)
            )
        }

        val previousAverage = currentModel.learnedBlockMinutes[block.name] ?: block.defaultMinutesPerBlock
        val updatedMinutes = (previousAverage * EMA_WEIGHT) + (measuredMinutes * (1.0 - EMA_WEIGHT))

        val updatedMap = currentModel.learnedBlockMinutes.toMutableMap()
        updatedMap[block.name] = updatedMinutes

        val newSessions = currentModel.totalSessionsAnalyzed + 1
        val calcChargeVelocity = 5.0 / updatedMinutes // percent per minute for this block

        return currentModel.copy(
            learnedBlockMinutes = updatedMap,
            totalSessionsAnalyzed = newSessions,
            averageChargeVelocityPercentPerMin = calcChargeVelocity,
            confidenceScore = min(0.98, currentModel.confidenceScore + 0.15)
        )
    }

    /**
     * Resets learned weights when user switches chargers or re-calibrates habits.
     */
    fun resetHabits(): PredictiveModelData {
        return PredictiveModelData(
            learnedBlockMinutes = emptyMap(),
            averageChargeVelocityPercentPerMin = 0.0,
            averageDischargePercentPerHour = 0.0,
            totalSessionsAnalyzed = 0,
            confidenceScore = 0.0,
            anomalyBudgetRemaining = 5,
            daytimeDischargePerHour = 0.0,
            nightDischargePerHour = 0.0
        )
    }
}
