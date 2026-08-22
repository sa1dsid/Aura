package com.aura.core.api

import com.aura.core.api.dto.EmailCredentialsDto
import com.aura.core.api.dto.GiftPopupSeenDto
import com.aura.core.api.dto.GoogleSignInRequestDto
import com.aura.core.api.dto.InviteApplyDto
import com.aura.core.api.dto.InviteStateDto
import com.aura.core.api.dto.MessageResponseDto
import com.aura.core.api.dto.PasswordResetRequestDto
import com.aura.core.api.dto.PreferenceUpdateDto
import com.aura.core.api.dto.PublicConfigDto
import com.aura.core.api.dto.TokenResponseDto
import com.aura.core.api.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AuraApi {

    @POST("api/v1/auth/register")
    suspend fun register(@Body credentials: EmailCredentialsDto): TokenResponseDto

    @POST("api/v1/auth/login")
    suspend fun login(@Body credentials: EmailCredentialsDto): TokenResponseDto

    @POST("api/v1/auth/google")
    suspend fun googleSignIn(@Body request: GoogleSignInRequestDto): TokenResponseDto

    @POST("api/v1/auth/password-reset/request")
    suspend fun requestPasswordReset(@Body request: PasswordResetRequestDto): MessageResponseDto

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

    @PATCH("api/v1/home/preferences")
    suspend fun updatePreferences(@Body request: PreferenceUpdateDto): PreferenceUpdateDto

    @GET("api/v1/home/config")
    suspend fun publicConfig(): PublicConfigDto
}
