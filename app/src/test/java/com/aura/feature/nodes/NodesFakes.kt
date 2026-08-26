package com.aura.feature.nodes

import com.aura.feature.nodes.data.remote.NodesRemoteDataSource
import com.aura.feature.nodes.data.remote.dto.FriendDto
import com.aura.feature.nodes.data.remote.dto.NodesSnapshotDto
import com.aura.feature.nodes.data.remote.dto.SocialLinkDto
import com.aura.feature.nodes.domain.model.Friend
import com.aura.feature.nodes.domain.model.FriendStatus
import com.aura.feature.nodes.domain.model.InviteOffer
import com.aura.feature.nodes.domain.model.NodesState
import com.aura.feature.nodes.domain.model.ReferralRewards
import com.aura.feature.nodes.domain.model.ReferralTier
import com.aura.feature.nodes.domain.model.SocialLink
import com.aura.feature.nodes.domain.model.SocialNetwork
import com.aura.feature.nodes.domain.model.TierRates
import com.aura.feature.nodes.domain.repository.NodesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

internal fun nodesSnapshot(
    handle: String = "syrex",
    inviteCode: String = Nodes.PERSONAL_CODE,
    inviteLink: String = Nodes.PERSONAL_URL,
    inviteQuote: String? = null,
    inviteShareText: String? = Nodes.SHARE_TEXT,
    friendsJoined: Int = 0,
    activeFriends: Int = 0,
    tier: String = "CORE_NODE",
    tierSparkPercent: Int = 15,
    tierWithdrawalPercent: Double = 5.0,
    nextTier: String? = null,
    friendsToNextTier: Int = 1,
    nextThreshold: Int? = null,
    earnedSpark: Long = 3_260,
    earnedIon: Long = 890,
    friends: List<FriendDto> = emptyList(),
    socials: List<SocialLinkDto> = emptyList(),
) = NodesSnapshotDto(
    handle = handle,
    inviteCode = inviteCode,
    inviteLink = inviteLink,
    inviteQuote = inviteQuote,
    inviteShareText = inviteShareText,
    friendsJoined = friendsJoined,
    activeFriends = activeFriends,
    tier = tier,
    tierSparkPercent = tierSparkPercent,
    tierWithdrawalPercent = tierWithdrawalPercent,
    nextTier = nextTier,
    friendsToNextTier = friendsToNextTier,
    nextThreshold = nextThreshold,
    earnedSpark = earnedSpark,
    earnedIon = earnedIon,
    friends = friends,
    socials = socials,
)

internal fun friendOf(
    id: String = "f1",
    name: String = "Alex K.",
    handle: String = "Alex K.",
    initials: String = "AK",
    spark: Long = 12_400,
    ion: Long = 1_840,
    status: FriendStatus = FriendStatus.EARNING,
) = Friend(
    id = id,
    name = name,
    handle = handle,
    initials = initials,
    spark = spark,
    ion = ion,
    status = status,
)

internal fun socialOf(
    network: SocialNetwork = SocialNetwork.DISCORD,
    webUrl: String = "https://discord.gg/ioaura",
    appUrl: String? = null,
) = SocialLink(network = network, webUrl = webUrl, appUrl = appUrl)

internal fun nodesState(
    handle: String = "syrex",
    invite: InviteOffer = InviteOffer(
        code = Nodes.PERSONAL_CODE,
        link = Nodes.PERSONAL_URL,
        quote = null,
        shareText = Nodes.SHARE_TEXT,
    ),
    friendsJoined: Int = 0,
    activeFriends: Int = 0,
    tier: ReferralTier = ReferralTier.CORE_NODE,
    tierRates: TierRates = TierRates(sparkPercent = 15, withdrawalPercent = 5.0),
    nextTier: ReferralTier? = ReferralTier.IONIC_PRIME,
    friendsToNextTier: Int = 1,
    rewards: ReferralRewards = ReferralRewards(spark = 3_260, ion = 890),
    friends: List<Friend> = emptyList(),
    socials: List<SocialLink> = emptyList(),
) = NodesState(
    handle = handle,
    invite = invite,
    friendsJoined = friendsJoined,
    activeFriends = activeFriends,
    tier = tier,
    tierRates = tierRates,
    nextTier = nextTier,
    friendsToNextTier = friendsToNextTier,
    rewards = rewards,
    friends = friends,
    socials = socials,
)

internal class FakeNodesRemoteDataSource(
    var snapshot: NodesSnapshotDto = nodesSnapshot(),
) : NodesRemoteDataSource {

    var failure: Throwable? = null

    var calls = 0
        private set

    override suspend fun fetchNodes(): NodesSnapshotDto {
        calls++
        failure?.let { throw it }
        return snapshot
    }
}

internal class RecordingNodesRepository : NodesRepository {

    private val state = MutableStateFlow<NodesState?>(null)

    var refreshes = 0
        private set

    override fun observeNodes(): Flow<NodesState> = state.filterNotNull()

    override suspend fun refresh() {
        refreshes++
    }

    fun emit(nodes: NodesState) {
        state.value = nodes
    }
}
