package com.sunitha.fittrack.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object HealthConnectManager {

    val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    fun isAvailable(context: Context): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun createPermissionLauncher() =
        PermissionController.createRequestPermissionResultContract()

    suspend fun hasPermissions(context: Context): Boolean {
        if (!isAvailable(context)) return false
        val client = HealthConnectClient.getOrCreate(context)
        return client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
    }

    suspend fun readTodaySteps(context: Context): Int {
        val client = HealthConnectClient.getOrCreate(context)
        val start  = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val end    = Instant.now()
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType      = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records.sumOf { it.count }.toInt()
        } catch (_: Exception) {
            0
        }
    }
}
