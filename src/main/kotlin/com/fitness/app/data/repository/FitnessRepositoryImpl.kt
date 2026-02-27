package com.fitness.app.data.repository

import com.fitness.app.FitnessAPI
import com.fitness.app.domain.model.CalorieMetrics
import com.fitness.app.domain.model.DailyHealthSummary
import com.fitness.app.domain.model.HeartRateMetrics
import com.fitness.app.domain.model.StepMetrics
import com.fitness.app.domain.model.WorkoutInfo
import com.fitness.app.domain.repository.IFitnessRepository

/**
 * Implementation of IFitnessRepository backed by FitnessAPI (SharedPreferences).
 * 
 * This wrapper allows ViewModels to depend on the IFitnessRepository interface
 * instead of the concrete FitnessAPI class. When Room DB is added later,
 * a new implementation can replace this without changing any ViewModel code.
 */
class FitnessRepositoryImpl(
    private val fitnessAPI: FitnessAPI
) : IFitnessRepository {

    // ── Steps ──────────────────────────────────────────────────────

    override fun getSteps(): StepMetrics {
        val data = fitnessAPI.getSteps()
        return StepMetrics(
            steps = data.steps,
            goal = data.goal,
            progress = data.progress
        )
    }

    override fun setSteps(steps: Int) {
        fitnessAPI.setSteps(steps)
    }

    override fun incrementSteps(count: Int) {
        fitnessAPI.incrementSteps(count)
    }

    // ── Calories ───────────────────────────────────────────────────

    override fun getCalories(): CalorieMetrics {
        val data = fitnessAPI.getCalories()
        return CalorieMetrics(
            calories = data.calories,
            goal = data.goal,
            progress = data.progress
        )
    }

    override fun setCalories(calories: Int) {
        fitnessAPI.setCalories(calories)
    }

    override fun addCalories(calories: Int) {
        fitnessAPI.addCalories(calories)
    }

    // ── Heart Rate ─────────────────────────────────────────────────

    override fun getHeartRate(): HeartRateMetrics {
        val data = fitnessAPI.getHeartRate()
        return HeartRateMetrics(
            currentBPM = data.currentBPM,
            averageBPM = data.averageBPM,
            minBPM = data.minBPM,
            maxBPM = data.maxBPM
        )
    }

    override fun setHeartRate(currentBPM: Int) {
        fitnessAPI.setHeartRate(currentBPM)
    }

    // ── Daily Summary ──────────────────────────────────────────────

    override fun getDailySummary(date: String?): DailyHealthSummary {
        val todayDate = date ?: getTodayDate()
        val data = fitnessAPI.getDailySummary(todayDate)
        return DailyHealthSummary(
            date = data.date,
            steps = data.totalSteps,
            calories = data.totalCalories,
            activeMinutes = data.activeTime,
            heartRateAvg = data.averageHeartRate,
            sleepHours = data.sleepDuration / 60f
        )
    }

    // ── Workouts ───────────────────────────────────────────────────

    override fun getWorkouts(): List<WorkoutInfo> {
        return fitnessAPI.getWorkouts().map { workout ->
            WorkoutInfo(
                id = workout.id,
                name = workout.name,
                type = workout.type,
                duration = workout.duration,
                caloriesBurned = workout.caloriesBurned,
                distance = workout.distance,
                intensity = workout.intensity,
                date = workout.date,
                notes = workout.notes
            )
        }
    }

    override fun addWorkout(workout: WorkoutInfo): WorkoutInfo {
        val result = fitnessAPI.addWorkout(
            name = workout.name,
            type = workout.type,
            duration = workout.duration,
            caloriesBurned = workout.caloriesBurned,
            distance = workout.distance,
            intensity = workout.intensity,
            notes = workout.notes
        )
        return WorkoutInfo(
            id = result.id,
            name = result.name,
            type = result.type,
            duration = result.duration,
            caloriesBurned = result.caloriesBurned,
            distance = result.distance,
            intensity = result.intensity,
            date = result.date,
            notes = result.notes
        )
    }

    override fun deleteWorkout(workoutId: String) {
        fitnessAPI.deleteWorkout(workoutId)
    }

    // ── Preferences ────────────────────────────────────────────────

    override fun getDailyStepGoal(): Int {
        return fitnessAPI.getUserPreferences().dailyStepGoal
    }

    override fun setDailyStepGoal(goal: Int) {
        fitnessAPI.setDailyStepGoal(goal)
    }

    override fun getDailyCalorieGoal(): Int {
        return fitnessAPI.getUserPreferences().dailyCalorieGoal
    }

    override fun setDailyCalorieGoal(goal: Int) {
        fitnessAPI.setDailyCalorieGoal(goal)
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private fun getTodayDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}
