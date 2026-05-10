package com.example.cityapp.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class TelemetrySyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return Result.success()
    }
}
