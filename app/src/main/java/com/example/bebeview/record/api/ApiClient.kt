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
    /** 로그인 토큰. 지표 업로드·공유 코드 발급 등 본인 확인이 필요한 요청에 Bearer 로 실어 보냄 */
    val token: String,
)

data class SendCodeRequest(val email: String)

/** 파일럿 단계(실제 메일 발송 전)라 서버가 인증번호(devCode)를 응답에 그대로 돌려줌 */
data class SendCodeResponse(val sent: Boolean, val devCode: String? = null)

data class VerifyCodeRequest(val email: String, val code: String)

/** 지금 로그인한 보호자의 토큰. 로그인·회원가입·앱 시작 때 채우고, 처음으로 돌아갈 때 비움 */
object AuthTokenHolder {
    @Volatile
    var token: String? = null
}

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

    /** 이메일로 인증번호 보내기 */
    @POST("api/auth/email/send-code")
    suspend fun sendEmailCode(@Body body: SendCodeRequest): SendCodeResponse

    /** 인증번호 확인. 성공 응답 본문은 쓰지 않음 (본문이 비어 있어도 되게 Unit) — 틀리면 400 으로 예외 */
    @POST("api/auth/email/verify")
    suspend fun verifyEmailCode(@Body body: VerifyCodeRequest): Unit
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
        // 로그인 토큰이 있으면 모든 요청에 실어 보냄 (지표 업로드·공유 코드 발급은 서버가 토큰을 확인함)
        .addInterceptor { chain ->
            val token = AuthTokenHolder.token
            val req = chain.request()
            chain.proceed(
                if (token.isNullOrBlank()) req
                else req.newBuilder().header("Authorization", "Bearer $token").build()
            )
        }
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    val api: BebeviewApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BebeviewApi::class.java)
}
