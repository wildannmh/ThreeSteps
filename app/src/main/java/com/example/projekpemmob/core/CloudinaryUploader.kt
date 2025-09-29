package com.example.projekpemmob.core

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object CloudinaryUploader {
    private const val CLOUD_NAME = "dqzkrbwoj"       // TODO ganti
    private const val UPLOAD_PRESET = "projek-pemmob" // TODO ganti
    private val client = OkHttpClient()

    fun uploadImageBlocking(context: Context, uri: Uri): String {
        val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open input")
        val temp = File.createTempFile("upload_", ".tmp", context.cacheDir)
        FileOutputStream(temp).use { out -> input.copyTo(out) }

        val url = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .addFormDataPart("file", temp.name, temp.asRequestBody("image/*".toMediaTypeOrNull()))
            .build()

        val req = Request.Builder().url(url).post(body).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Upload failed: ${resp.code}")
            val json = JSONObject(resp.body?.string().orEmpty())
            return json.getString("secure_url")
        }
    }
}