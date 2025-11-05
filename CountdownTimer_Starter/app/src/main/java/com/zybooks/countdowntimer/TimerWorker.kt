package com.zybooks.countdowntimer

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zybooks.countdowntimer.ui.timerText
import kotlinx.coroutines.delay

const val KEY_MILLIS_REMAINING = "com.zybooks.countdowntimer.MILLIS_LEFT"

class TimerWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        // Get remaining milliseconds from MainActivity
        var remainingMillis = inputData.getLong(KEY_MILLIS_REMAINING, 0)

        // Can't continue without remaining time
        if (remainingMillis == 0L) {
            return Result.failure()
        }

        // Create notification channel for all notifications
        createTimerNotificationChannel()

        // Post notifications every second until no time remains
        while (remainingMillis > 0) {
            postTimerNotification(timerText(remainingMillis))
            delay(1000)
            remainingMillis -= 1000
        }

        // Post final notification
        postTimerNotification("Timer is finished!")

        return Result.success()
    }

    private fun createTimerNotificationChannel() {
        // TODO: Create a notification channel
    }

    private fun postTimerNotification(text: String) {
        // TODO: Create a notification

        Log.d("TimerWorker", text)
    }
}