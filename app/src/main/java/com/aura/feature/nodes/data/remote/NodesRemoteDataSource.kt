package com.aura.feature.nodes.data.remote

import com.aura.core.api.AuraApi
import com.aura.core.config.AppConfigRepository
import com.aura.feature.nodes.data.remote.dto.FriendDto
import com.aura.feature.nodes.data.remote.dto.NodesSnapshotDto
import com.aura.feature.nodes.data.remote.dto.SocialLinkDto
import com.aura.feature.nodes.domain.model.SocialNetwork
import com.aura.feature.onboarding.data.local.SessionStore
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

interface NodesRemoteDataSource {
    suspend fun fetchNodes(): NodesSnapshotDto
}

@Singleton
class ApiNodesRemoteDataSource @Inject constructor(
    private val api: AuraApi,
    private val sessionStore: SessionStore,
    private val appConfigRepository: AppConfigRepository,
) : NodesRemoteDataSource {

    override suspend fun fetchNodes(): NodesSnapshotDto {
        appConfigRepository.refresh()

        val nodes = api.nodes()
        val invite = try {
            api.inviteState()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            null
        }

        val account = sessionStore.account.value
        val configuredUrls = appConfigRepository.config.value.socialLinks
            .filter { it.url.isNotBlank() }
            .associate { it.network.name to it.url }

        val socials = SocialNetwork.entries.map { network ->
            SocialLinkDto(
                network = network.name,
                webUrl = configuredUrls[network.name].orEmpty(),
                appUrl = null,
            )
        }

        return NodesSnapshotDto(
            handle = account?.handle.orEmpty(),
            inviteCode = invite?.personalCode.orEmpty(),
            inviteLink = invite?.personalUrl ?: account?.inviteLink.orEmpty(),
            inviteQuote = null,
            inviteShareText = invite?.shareText,
            friendsJoined = nodes.friendsJoined,
            activeFriends = nodes.activeFriends,
            tier = nodes.tier,
            tierSparkPercent = nodes.sparkReferralPercent,
            tierWithdrawalPercent = nodes.ionReferralPercentStage2.toDouble(),
            nextTier = null,
            friendsToNextTier = nodes.moreForNextTier
                ?: nodes.nodeStatus.progressTarget
                    ?.minus(nodes.nodeStatus.progressCurrent)
                    ?.coerceAtLeast(0)
                ?: nodes.nextThreshold?.minus(nodes.activeFriends)?.coerceAtLeast(0)
                ?: 0,
            nextThreshold = nodes.nextThreshold,
            earnedSpark = nodes.earnedFromReferrals.spark.toDoubleOrNull()?.toLong() ?: 0,
            earnedIon = nodes.earnedFromReferrals.ion,
            friends = nodes.friends.map { friend ->
                FriendDto(
                    id = friend.id.toString(),
                    name = friend.displayName,
                    handle = friend.displayName,
                    spark = friend.ownSpark.toDoubleOrNull()?.toLong() ?: 0,
                    ion = friend.ownIon,
                    status = friend.status,
                )
            },
            socials = socials,
        )
    }
}
