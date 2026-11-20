package com.example.bebeview.record.api

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// 베베뷰 백엔드(EC2) 연동. 파일럿 단계라 IP를 직접 박아둠 — 정식 배포 때 도메인+HTTPS로 교체.
private const val BASE_URL = "http://43.201.67.12:8080/"

data class MetricUploadRequest(
    val childId: String,
    val taskType: String,
    val metricName: String,
    val value: Double,
    // 이 순간의 교정 월령(숫자)만 보냄. 아이 생년월일/조산여부 자체는 서버로 안 보냄.
    val correctedAgeMonths: Double? = null,
)

data class ServerMetric(
    val id: String,
    val childId: String,
    val taskType: String,
    val metricName: String,
    val value: Double,
    val correctedAgeMonths: Double? = null,
    val recordedAt: String,
)

data class ShareCodeResponse(
    val code: String,
    val expiresAt: String,
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class AuthResponse(
    val parentId: String,
    val name: String,
    val email: String,
)

interface BebeviewApi {
    @POST("api/metrics")
    suspend fun uploadMetric(@Body body: MetricUploadRequest): ServerMetric

    @POST("api/share-codes")
    suspend fun createShareCode(@Query("childId") childId: String): ShareCodeResponse

    @GET("api/share-codes/{code}")
    suspend fun fetchByShareCode(@Path("code") code: String): List<ServerMetric>

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse
}

/** 서버가 내려준 에러 메시지({"error": "..."}) 꺼내기. 실패하면 일반 안내문구 */
fun errorMessageOf(e: Throwable): String {
    if (e is HttpException) {
        val body = e.response()?.errorBody()?.string()
        if (!body.isNullOrBlank()) {
            return try {
                com.google.gson.JsonParser.parseString(body).asJsonObject.get("error")?.asString
                    ?: "오류가 발생했어요"
            } catch (_: Exception) {
                "오류가 발생했어요"
            }
        }
    }
    return "네트워크 오류가 발생했어요. 잠시 후 다시 시도해주세요"
}

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    val api: BebeviewApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BebeviewApi::class.java)
}
