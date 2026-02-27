package com.fitness.app.domain.repository

import com.fitness.app.core.util.Result
import com.fitness.app.domain.model.CalorieMetrics
import com.fitness.app.domain.model.DailyHealthSummary
import com.fitness.app.domain.model.HeartRateMetrics
import com.fitness.app.domain.model.StepMetrics
import com.fitness.app.domain.model.WorkoutInfo

/**
 * Repository interface for local fitness data operations.
 * Abstracts the data source so ViewModels don't depend on FitnessAPI directly.
 * Can later be backed by Room DB instead of SharedPreferences.
 */
interface IFitnessRepository {

    // ── Steps ──────────────────────────────────────────────────────

    fun getSteps(): StepMetrics
    fun setSteps(steps: Int)
    fun incrementSteps(count: Int = 1)

    // ── Calories ───────────────────────────────────────────────────

    fun getCalories(): CalorieMetrics
    fun setCalories(calories: Int)
    fun addCalories(calories: Int)

    // ── Heart Rate ─────────────────────────────────────────────────

    fun getHeartRate(): HeartRateMetrics
    fun setHeartRate(currentBPM: Int)

    // ── Daily Summary ──────────────────────────────────────────────

    fun getDailySummary(date: String? = null): DailyHealthSummary

    // ── Workouts ───────────────────────────────────────────────────

    fun getWorkouts(): List<WorkoutInfo>
    fun addWorkout(workout: WorkoutInfo): WorkoutInfo
    fun deleteWorkout(workoutId: String)

    // ── Preferences ────────────────────────────────────────────────

    fun getDailyStepGoal(): Int
    fun setDailyStepGoal(goal: Int)
    fun getDailyCalorieGoal(): Int
    fun setDailyCalorieGoal(goal: Int)
}
