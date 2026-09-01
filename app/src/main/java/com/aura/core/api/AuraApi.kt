package com.aura.core.api

import com.aura.core.api.dto.BatteryOptimizationDto
import com.aura.core.api.dto.BonusCongratulationsSeenDto
import com.aura.core.api.dto.BonusTeaserSeenDto
import com.aura.core.api.dto.CityDto
import com.aura.core.api.dto.DashboardDto
import com.aura.core.api.dto.EarningStateDto
import com.aura.core.api.dto.EarningStateUpdateDto
import com.aura.core.api.dto.EmailCredentialsDto
import com.aura.core.api.dto.GiftPopupSeenDto
import com.aura.core.api.dto.GoogleSignInRequestDto
import com.aura.core.api.dto.HeartbeatDto
import com.aura.core.api.dto.IntegrityChallengeDto
import com.aura.core.api.dto.InviteApplyDto
import com.aura.core.api.dto.InviteStateDto
import com.aura.core.api.dto.LocationUpdateDto
import com.aura.core.api.dto.MeshDto
import com.aura.core.api.dto.MessageResponseDto
import com.aura.core.api.dto.NetworkStateDto
import com.aura.core.api.dto.NetworkStateUpdateDto
import com.aura.core.api.dto.NetworkSummaryDto
import com.aura.core.api.dto.NewsDto
import com.aura.core.api.dto.NodesDto
import com.aura.core.api.dto.PasswordResetConfirmDto
import com.aura.core.api.dto.PasswordResetRequestDto
import com.aura.core.api.dto.PingCreateDto
import com.aura.core.api.dto.PingDto
import com.aura.core.api.dto.PreferenceUpdateDto
import com.aura.core.api.dto.PromoDto
import com.aura.core.api.dto.PublicConfigDto
import com.aura.core.api.dto.PushTokenDeleteDto
import com.aura.core.api.dto.PushTokenDeleteResponseDto
import com.aura.core.api.dto.PushTokenRegisterDto
import com.aura.core.api.dto.PushTokenResponseDto
import com.aura.core.api.dto.TapFinishDto
import com.aura.core.api.dto.TapStartDto
import com.aura.core.api.dto.TapStateDto
import com.aura.core.api.dto.TerminalDto
import com.aura.core.api.dto.TokenResponseDto
import com.aura.core.api.dto.TransactionDto
import com.aura.core.api.dto.UnreadNewsDto
import com.aura.core.api.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AuraApi {

    @POST("api/v1/auth/register")
    suspend fun register(@Body credentials: EmailCredentialsDto): TokenResponseDto

    @POST("api/v1/auth/login")
    suspend fun login(@Body credentials: EmailCredentialsDto): TokenResponseDto

    @POST("api/v1/auth/google")
    suspend fun googleSignIn(@Body request: GoogleSignInRequestDto): TokenResponseDto

    @POST("api/v1/auth/password-reset/request")
    suspend fun requestPasswordReset(@Body request: PasswordResetRequestDto): MessageResponseDto

    @POST("api/v1/auth/password-reset/confirm")
    suspend fun confirmPasswordReset(@Body request: PasswordResetConfirmDto): MessageResponseDto

    @GET("api/v1/auth/me")
    suspend fun currentUser(): UserDto

    @DELETE("api/v1/auth/me")
    suspend fun deleteCurrentUser()

    @GET("api/v1/onboarding/invite")
    suspend fun inviteState(): InviteStateDto

    @POST("api/v1/onboarding/invite/apply")
    suspend fun applyInvite(@Body request: InviteApplyDto): InviteStateDto

    @POST("api/v1/onboarding/invite/skip")
    suspend fun skipInvite(): InviteStateDto

    @POST("api/v1/onboarding/gift-popup/seen")
    suspend fun markGiftPopupSeen(): GiftPopupSeenDto

    @POST("api/v1/onboarding/bonus-teaser/seen")
    suspend fun markBonusTeaserSeen(): BonusTeaserSeenDto

    @POST("api/v1/onboarding/bonus-congratulations/seen")
    suspend fun markBonusCongratulationSeen(): BonusCongratulationsSeenDto

    @GET("api/v1/home/dashboard")
    suspend fun dashboard(): DashboardDto

    @POST("api/v1/home/tap/start")
    suspend fun startTap(@Body request: TapStartDto): TapStateDto

    @POST("api/v1/home/tap/integrity-challenge")
    suspend fun issueIntegrityChallenge(): IntegrityChallengeDto

    @POST("api/v1/home/tap/{sessionId}/heartbeat")
    suspend fun tapHeartbeat(@Path("sessionId") sessionId: String): TapStateDto

    @POST("api/v1/home/tap/{sessionId}/finish")
    suspend fun finishTap(
        @Path("sessionId") sessionId: String,
        @Body request: TapFinishDto,
    ): TapStateDto

    @GET("api/v1/home/battery-optimization")
    suspend fun batteryOptimization(): BatteryOptimizationDto

    @POST("api/v1/home/battery-optimization/decline")
    suspend fun declineBatteryOptimization(): BatteryOptimizationDto

    @POST("api/v1/home/battery-optimization/confirmed-disabled")
    suspend fun confirmBatteryOptimizationDisabled(): BatteryOptimizationDto

    @PUT("api/v1/home/earning-state")
    suspend fun updateEarningState(@Body request: EarningStateUpdateDto): EarningStateDto

    @POST("api/v1/home/heartbeat")
    suspend fun heartbeat(): HeartbeatDto

    @PUT("api/v1/home/location")
    suspend fun updateLocation(@Body request: LocationUpdateDto): CityDto

    @GET("api/v1/home/mesh")
    suspend fun mesh(): MeshDto

    @PATCH("api/v1/home/preferences")
    suspend fun updatePreferences(@Body request: PreferenceUpdateDto): PreferenceUpdateDto

    @POST("api/v1/home/push-tokens")
    suspend fun registerPushToken(@Body request: PushTokenRegisterDto): PushTokenResponseDto

    @HTTP(method = "DELETE", path = "api/v1/home/push-tokens", hasBody = true)
    suspend fun removePushToken(@Body request: PushTokenDeleteDto): PushTokenDeleteResponseDto

    @GET("api/v1/home/news")
    suspend fun news(): List<NewsDto>

    @POST("api/v1/home/news/open")
    suspend fun openNews(): UnreadNewsDto

    @GET("api/v1/home/config")
    suspend fun publicConfig(): PublicConfigDto

    @GET("api/v1/nodes")
    suspend fun nodes(): NodesDto

    @GET("api/v1/terminal")
    suspend fun terminal(): TerminalDto

    @GET("api/v1/terminal/transactions")
    suspend fun transactions(@Query("limit") limit: Int): List<TransactionDto>

    @GET("api/v1/terminal/promo-codes")
    suspend fun promoCodes(): List<PromoDto>

    @GET("api/v1/network/measurements")
    suspend fun measurements(): List<PingDto>

    @POST("api/v1/network/measurements")
    suspend fun addMeasurement(@Body request: PingCreateDto): PingDto

    @PUT("api/v1/network/state")
    suspend fun updateNetworkState(@Body request: NetworkStateUpdateDto): NetworkStateDto

    @GET("api/v1/network/summary")
    suspend fun networkSummary(): NetworkSummaryDto
}
