package com.aura.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.aura.core.api.Bodies
import com.aura.core.api.RoutingApiServer
import com.aura.core.auth.GoogleSignInClient
import com.aura.core.auth.TokenStore
import com.aura.core.config.AppConfigRepository
import com.aura.core.push.PushTokenRepository
import com.aura.feature.onboarding.data.attribution.InstallReferrerSource
import com.aura.feature.onboarding.data.attribution.InviteAttributionStore
import com.aura.feature.onboarding.data.local.SessionStore
import com.aura.feature.onboarding.data.remote.ApiOnboardingRemoteDataSource
import com.aura.feature.onboarding.data.repository.AuthRepositoryImpl
import com.aura.feature.onboarding.data.repository.BootRepositoryImpl
import com.aura.feature.onboarding.data.repository.InviteRepositoryImpl
import com.aura.feature.onboarding.data.repository.OnboardingFlagsRepositoryImpl
import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthProvider
import com.aura.feature.onboarding.domain.usecase.ApplyInviteCodeUseCase
import com.aura.feature.onboarding.domain.usecase.BootstrapUseCase
import com.aura.feature.onboarding.domain.usecase.ContinueWithGoogleUseCase
import com.aura.feature.onboarding.domain.usecase.ObserveInviteAttributionUseCase
import com.aura.feature.onboarding.domain.usecase.RequestPasswordResetUseCase
import com.aura.feature.onboarding.domain.usecase.ResolveStartDestinationUseCase
import com.aura.feature.onboarding.domain.usecase.SignInUseCase
import com.aura.feature.onboarding.domain.usecase.SignUpUseCase
import com.aura.feature.onboarding.domain.usecase.SkipInviteUseCase
import com.aura.feature.onboarding.presentation.auth.AuthViewModel
import com.aura.feature.onboarding.presentation.bonus.WelcomeBonusViewModel
import com.aura.feature.onboarding.presentation.invite.InviteViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections

internal class FakeInstallReferrerSource(private val code: String? = null) : InstallReferrerSource {
    var reads = 0

    override suspend fun inviteCode(): String? {
        reads++
        return code
    }
}

internal class OnboardingStack(referrerCode: String? = null) {

    val server = RoutingApiServer()

    private val ioDispatcher = Dispatchers.Unconfined

    private val stackScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private val viewModelStore = ViewModelStore()

    private val tracked = mutableListOf<ViewModel>()

    private var storedToken: String? = null

    val savedToken: String?
        get() = storedToken

    var storedExpiresIn: Int? = null
        private set

    var pushRefreshes = 0
        private set

    val tokenStore: TokenStore = mockk()

    val pushTokenRepository: PushTokenRepository = mockk()

    val googleSignInClient: GoogleSignInClient = mockk()

    val activityContext: Context = mockk(relaxed = true)

    val referrerSource = FakeInstallReferrerSource(referrerCode)

    init {
        coEvery { tokenStore.token() } answers { storedToken }
        every { tokenStore.blockingToken() } answers { storedToken }
        coEvery { tokenStore.save(any(), any()) } answers {
            storedToken = firstArg()
            storedExpiresIn = secondArg()
        }
        coEvery { tokenStore.clear() } answers { storedToken = null }
        coEvery { pushTokenRepository.refresh() } answers { pushRefreshes++ }
        coEvery { googleSignInClient.warmUp() } answers { }
        coEvery { googleSignInClient.idToken(any()) } returns GOOGLE_ID_TOKEN

        server.always(Paths.CONFIG, body = Bodies.CONFIG)
        server.always(Paths.MESH, body = Server.mesh())
    }

    val sessionStore = SessionStore()

    val attributionStorage = FakeInviteAttributionStorage()

    val attributionStore = InviteAttributionStore(referrerSource, attributionStorage)

    val appConfigRepository = AppConfigRepository(server.api, ioDispatcher)

    val remote = ApiOnboardingRemoteDataSource(server.api, tokenStore, appConfigRepository)

    val authRepository = AuthRepositoryImpl(
        remote = remote,
        sessionStore = sessionStore,
        tokenStore = tokenStore,
        pushTokenRepository = pushTokenRepository,
        applicationScope = stackScope,
        ioDispatcher = ioDispatcher,
    )

    val inviteRepository = InviteRepositoryImpl(remote, attributionStore, ioDispatcher)

    val flagsRepository = OnboardingFlagsRepositoryImpl(remote, ioDispatcher)

    val bootRepository = BootRepositoryImpl(remote, ioDispatcher)

    val resolveStartDestination = ResolveStartDestinationUseCase(authRepository)

    val bootstrap = BootstrapUseCase(bootRepository)

    fun signedInWithToken(token: String = "restored.session.token") {
        storedToken = token
    }

    fun openSession(
        id: String = "39",
        email: String = "smoke@auratest.dev",
        handle: String = "smoke",
        promoCode: String = "3ZBCZ9MA",
    ): Account {
        val account = Account(
            id = id,
            email = email,
            handle = handle,
            inviteLink = "https://ioaura.app/i/$promoCode",
            authProvider = AuthProvider.EMAIL,
        )
        signedInWithToken()
        sessionStore.open(account)
        return account
    }

    fun <T> eventsOf(flow: Flow<T>): List<T> {
        val collected = Collections.synchronizedList(mutableListOf<T>())
        stackScope.launch { flow.collect { collected += it } }
        return collected
    }

    fun authViewModel(): AuthViewModel = track(
        "auth",
        AuthViewModel(
            signIn = SignInUseCase(authRepository),
            signUp = SignUpUseCase(authRepository),
            continueWithGoogle = ContinueWithGoogleUseCase(authRepository),
            requestPasswordReset = RequestPasswordResetUseCase(authRepository),
            googleSignInClient = googleSignInClient,
        ),
    )

    fun inviteViewModel(): InviteViewModel = track(
        "invite",
        InviteViewModel(
            authRepository = authRepository,
            flagsRepository = flagsRepository,
            observeInviteAttribution = ObserveInviteAttributionUseCase(inviteRepository),
            applyInviteCode = ApplyInviteCodeUseCase(inviteRepository),
            skipInvite = SkipInviteUseCase(inviteRepository),
        ),
    )

    fun bonusViewModel(): WelcomeBonusViewModel =
        track("bonus", WelcomeBonusViewModel(authRepository, flagsRepository))

    suspend fun close() {
        withTimeoutOrNull(SHUTDOWN_MILLIS) {
            tracked.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() }
            stackScope.coroutineContext.job.cancelAndJoin()
        }
        viewModelStore.clear()
        server.shutdown()
    }

    private fun <T : ViewModel> track(key: String, viewModel: T): T {
        viewModelStore.put(key, viewModel)
        tracked += viewModel
        return viewModel
    }

    companion object {
        const val GOOGLE_ID_TOKEN = "google.id.token"

        private const val SHUTDOWN_MILLIS = 5_000L
    }
}
