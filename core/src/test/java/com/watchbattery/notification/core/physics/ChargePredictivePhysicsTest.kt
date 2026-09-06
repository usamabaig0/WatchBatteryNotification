package com.watchbattery.notification.core.physics

import org.junit.Assert.*
import org.junit.Test

class ChargePredictivePhysicsTest {

    @Test
    fun testChargeBlockLookup() {
        assertEquals(ChargeBlock.B_0_5, ChargeBlock.forPercent(0))
        assertEquals(ChargeBlock.B_0_5, ChargeBlock.forPercent(4))
        assertEquals(ChargeBlock.B_40_45, ChargeBlock.forPercent(42))
        assertEquals(ChargeBlock.B_80_85, ChargeBlock.forPercent(80))
        assertEquals(ChargeBlock.B_95_100, ChargeBlock.forPercent(99))
        assertEquals(ChargeBlock.B_95_100, ChargeBlock.forPercent(100))
    }

    @Test
    fun testTimeToTargetCalculation() {
        val model = PredictiveModelData()

        // 0 to 100%
        val minsFrom0To100 = ChargePredictivePhysics.calculateMinutesToTarget(0, 100, model)
        assertTrue("Total charge time should be positive", minsFrom0To100 > 60)

        // 80 to 100% (CV trickle zone should be slower per % than 20 to 40%)
        val minsFastZone = ChargePredictivePhysics.calculateMinutesToTarget(20, 40, model)
        val minsSlowZone = ChargePredictivePhysics.calculateMinutesToTarget(80, 100, model)
        assertTrue("Trickle charge zone should take longer than linear fast charge zone", minsSlowZone > minsFastZone)

        // Already at or above target
        assertEquals(0, ChargePredictivePhysics.calculateMinutesToTarget(85, 80, model))
    }

    @Test
    fun testDischargeCalculation() {
        val model = PredictiveModelData()

        val (dayHours, dayMins) = ChargePredictivePhysics.calculateDischargeTimeRemaining(80, false, model)
        val (nightHours, nightMins) = ChargePredictivePhysics.calculateDischargeTimeRemaining(80, true, model)

        assertTrue("Daytime battery duration should be positive", dayHours > 0)
        assertTrue("Night sleep duration should be significantly longer than day active", nightHours > dayHours)
    }

    @Test
    fun testEmaUpdatingAndAnomalyRejection() {
        var model = PredictiveModelData()

        // Valid update: 2.0 minutes for block B_0_5
        val updated = ChargePredictivePhysics.updateBlockWithSample(ChargeBlock.B_0_5, 2.0, model)
        assertTrue("Learned block map should contain updated value", updated.learnedBlockMinutes.containsKey("B_0_5"))
        assertEquals(39, updated.totalSessionsAnalyzed)

        // Extreme anomaly (e.g. 0.05 min or 120 min) should be rejected and consume anomaly budget
        val anomalyModel = ChargePredictivePhysics.updateBlockWithSample(ChargeBlock.B_0_5, 120.0, updated)
        assertEquals(4, anomalyModel.anomalyBudgetRemaining)
    }

    @Test
    fun testHabitReset() {
        val reset = ChargePredictivePhysics.resetHabits()
        assertTrue(reset.learnedBlockMinutes.isEmpty())
        assertEquals(0, reset.totalSessionsAnalyzed)
        assertEquals(5, reset.anomalyBudgetRemaining)
    }
}
