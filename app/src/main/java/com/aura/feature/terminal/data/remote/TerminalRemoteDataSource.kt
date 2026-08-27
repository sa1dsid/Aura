package com.aura.feature.terminal.data.remote

import com.aura.core.api.AuraApi
import com.aura.core.api.dto.TerminalDto
import javax.inject.Inject
import javax.inject.Singleton

interface TerminalRemoteDataSource {

    suspend fun terminal(): TerminalDto
}

@Singleton
class ApiTerminalRemoteDataSource @Inject constructor(
    private val api: AuraApi,
) : TerminalRemoteDataSource {

    override suspend fun terminal(): TerminalDto = api.terminal()
}
