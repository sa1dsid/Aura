package com.aura.feature.terminal.data.remote

import com.aura.core.api.AuraApi
import com.aura.core.api.dto.PromoDto
import com.aura.core.api.dto.TerminalDto
import com.aura.core.api.dto.TransactionDto
import javax.inject.Inject
import javax.inject.Singleton

interface TerminalRemoteDataSource {
    suspend fun terminal(): TerminalDto

    suspend fun transactions(): List<TransactionDto>

    suspend fun promoCodes(): List<PromoDto>
}

@Singleton
class ApiTerminalRemoteDataSource @Inject constructor(
    private val api: AuraApi,
) : TerminalRemoteDataSource {

    override suspend fun terminal(): TerminalDto = api.terminal()

    override suspend fun transactions(): List<TransactionDto> = api.transactions()

    override suspend fun promoCodes(): List<PromoDto> = api.promoCodes()
}
