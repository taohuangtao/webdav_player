package com.tdull.webdavviewer.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tdull.webdavviewer.app.data.model.UploadConflictPolicy
import com.tdull.webdavviewer.app.data.model.UploadStatus
import com.tdull.webdavviewer.app.data.model.UploadTask
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.uploadsDataStore: DataStore<Preferences> by preferencesDataStore(name = "uploads")

/**
 * 使用 DataStore 持久保存后台上传任务。
 */
@Singleton
class UploadsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val UPLOADS_KEY = stringPreferencesKey("uploads_list")
    }

    fun getUploads(): Flow<List<UploadTask>> = context.uploadsDataStore.data.map { preferences ->
        parseUploadTasks(preferences[UPLOADS_KEY] ?: "[]")
    }

    suspend fun getUpload(id: String): UploadTask? {
        return getUploads().first().find { it.id == id }
    }

    suspend fun addUploads(tasks: List<UploadTask>) {
        if (tasks.isEmpty()) return
        context.uploadsDataStore.edit { preferences ->
            val uploads = parseUploadTasks(preferences[UPLOADS_KEY] ?: "[]").toMutableList()
            val existingIds = uploads.map { it.id }.toSet()
            uploads.addAll(0, tasks.filterNot { it.id in existingIds })
            preferences[UPLOADS_KEY] = serializeUploadTasks(uploads)
        }
    }

    suspend fun updateUpload(id: String, transform: (UploadTask) -> UploadTask) {
        context.uploadsDataStore.edit { preferences ->
            val uploads = parseUploadTasks(preferences[UPLOADS_KEY] ?: "[]")
            val updated = uploads.map { task ->
                if (task.id == id) transform(task).copy(updatedAt = System.currentTimeMillis()) else task
            }
            preferences[UPLOADS_KEY] = serializeUploadTasks(updated)
        }
    }

    suspend fun removeUpload(id: String) {
        context.uploadsDataStore.edit { preferences ->
            val uploads = parseUploadTasks(preferences[UPLOADS_KEY] ?: "[]")
                .filterNot { it.id == id }
            preferences[UPLOADS_KEY] = serializeUploadTasks(uploads)
        }
    }

    suspend fun clearFinishedUploads() {
        context.uploadsDataStore.edit { preferences ->
            val uploads = parseUploadTasks(preferences[UPLOADS_KEY] ?: "[]")
                .filterNot { it.isFinished }
            preferences[UPLOADS_KEY] = serializeUploadTasks(uploads)
        }
    }

    private fun parseUploadTasks(json: String): List<UploadTask> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                UploadTask(
                    id = obj.optString("id"),
                    uriString = obj.optString("uriString"),
                    fileName = obj.optString("fileName"),
                    mimeType = obj.optString("mimeType").ifBlank { null },
                    fileSize = obj.optLong("fileSize", -1L),
                    serverId = obj.optString("serverId"),
                    serverName = obj.optString("serverName"),
                    serverUrl = obj.optString("serverUrl"),
                    username = obj.optString("username"),
                    password = obj.optString("password"),
                    targetDirectory = obj.optString("targetDirectory", "/"),
                    targetPath = obj.optString("targetPath"),
                    tempPath = obj.optString("tempPath"),
                    conflictPolicy = obj.optEnum("conflictPolicy", UploadConflictPolicy.SKIP),
                    status = obj.optEnum("status", UploadStatus.QUEUED),
                    uploadedBytes = obj.optLong("uploadedBytes", 0L),
                    totalBytes = obj.optLong("totalBytes", obj.optLong("fileSize", -1L)),
                    errorMessage = obj.optString("errorMessage").ifBlank { null },
                    workName = obj.optString("workName", "upload_task_${obj.optString("id")}"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeUploadTasks(tasks: List<UploadTask>): String {
        val array = JSONArray()
        tasks.forEach { task ->
            val obj = JSONObject().apply {
                put("id", task.id)
                put("uriString", task.uriString)
                put("fileName", task.fileName)
                put("mimeType", task.mimeType ?: "")
                put("fileSize", task.fileSize)
                put("serverId", task.serverId)
                put("serverName", task.serverName)
                put("serverUrl", task.serverUrl)
                put("username", task.username)
                put("password", task.password)
                put("targetDirectory", task.targetDirectory)
                put("targetPath", task.targetPath)
                put("tempPath", task.tempPath)
                put("conflictPolicy", task.conflictPolicy.name)
                put("status", task.status.name)
                put("uploadedBytes", task.uploadedBytes)
                put("totalBytes", task.totalBytes)
                put("errorMessage", task.errorMessage ?: "")
                put("workName", task.workName)
                put("createdAt", task.createdAt)
                put("updatedAt", task.updatedAt)
            }
            array.put(obj)
        }
        return array.toString()
    }
}

private inline fun <reified T : Enum<T>> JSONObject.optEnum(key: String, default: T): T {
    val value = optString(key)
    return enumValues<T>().firstOrNull { it.name == value } ?: default
}
