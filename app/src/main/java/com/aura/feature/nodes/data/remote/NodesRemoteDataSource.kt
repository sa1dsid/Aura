package com.aura.feature.nodes.data.remote

import com.aura.core.api.AuraApi
import com.aura.core.api.dto.InviteStateDto
import com.aura.core.api.dto.NodesDto
import javax.inject.Inject
import javax.inject.Singleton

interface NodesRemoteDataSource {

    suspend fun nodes(): NodesDto

    suspend fun inviteState(): InviteStateDto
}

@Singleton
class ApiNodesRemoteDataSource @Inject constructor(
    private val api: AuraApi,
) : NodesRemoteDataSource {

    override suspend fun nodes(): NodesDto = api.nodes()

    override suspend fun inviteState(): InviteStateDto = api.inviteState()
}
