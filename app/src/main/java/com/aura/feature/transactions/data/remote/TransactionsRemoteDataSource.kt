package com.aura.feature.transactions.data.remote

import com.aura.core.api.AuraApi
import com.aura.core.api.dto.TransactionDto
import javax.inject.Inject
import javax.inject.Singleton

interface TransactionsRemoteDataSource {

    suspend fun transactions(): List<TransactionDto>
}

@Singleton
class ApiTransactionsRemoteDataSource @Inject constructor(
    private val api: AuraApi,
) : TransactionsRemoteDataSource {

    override suspend fun transactions(): List<TransactionDto> = api.transactions()
}
