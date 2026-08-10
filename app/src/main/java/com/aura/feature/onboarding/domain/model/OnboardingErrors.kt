package com.aura.feature.onboarding.domain.model

class AuthException(val failure: AuthFailure) : Exception(failure.name)

class InviteException(val failure: InviteFailure) : Exception(failure.name)
