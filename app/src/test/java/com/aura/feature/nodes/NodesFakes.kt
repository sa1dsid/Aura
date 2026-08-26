package com.aura.feature.nodes

import com.aura.core.api.dto.InviteStateDto
import com.aura.core.api.dto.NodeFriendDto
import com.aura.core.api.dto.NodeStatusDto
import com.aura.core.api.dto.NodesDto
import com.aura.core.api.dto.ReferralEarningsDto
import com.aura.core.config.AppConfig
import com.aura.feature.nodes.data.remote.NodesRemoteDataSource
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
import com.aura.feature.onboarding.domain.model.Account
import com.aura.feature.onboarding.domain.model.AuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import com.aura.core.config.SocialLink as ConfigSocialLink
import com.aura.core.config.SocialNetwork as ConfigSocialNetwork

internal fun nodesDto(
    friendsJoined: Int = 0,
    activeFriends: Int = 0,
    tier: String = Nodes.TIER_CORE_NODE,
    nextThreshold: Int? = null,
    moreForNextTier: Int? = 1,
    nodeStatus: NodeStatusDto = NodeStatusDto(),
    sparkReferralPercent: Int = 15,
    ionReferralPercentStage2: Double = 5.0,
    earnedSpark: String = "3260.000000",
    earnedIon: Long = 890,
    friends: List<NodeFriendDto> = emptyList(),
) = NodesDto(
    friendsJoined = friendsJoined,
    activeFriends = activeFriends,
    tier = tier,
    nextThreshold = nextThreshold,
    moreForNextTier = moreForNextTier,
    nodeStatus = nodeStatus,
    sparkReferralPercent = sparkReferralPercent,
    ionReferralPercentStage2 = ionReferralPercentStage2,
    earnedFromReferrals = ReferralEarningsDto(spark = earnedSpark, ion = earnedIon),
    friends = friends,
)

internal fun friendDto(
    id: Int = 1,
    displayName: String = "Daniel R.",
    status: String = Nodes.STATUS_SPARK_ONLY,
    ownSpark: String = "54200.000000",
    ownIon: Long = 0,
) = NodeFriendDto(
    id = id,
    displayName = displayName,
    status = status,
    ownSpark = ownSpark,
    ownIon = ownIon,
)

internal fun inviteDto(
    personalCode: String = Nodes.PERSONAL_CODE,
    personalUrl: String = Nodes.PERSONAL_URL,
    shareText: String = Nodes.SHARE_TEXT,
) = InviteStateDto(
    decision = "applied",
    personalCode = personalCode,
    personalUrl = personalUrl,
    shareText = shareText,
)

internal fun accountOf(
    handle: String = "syrex",
    inviteLink: String = Nodes.ACCOUNT_LINK,
) = Account(
    id = "39",
    email = "smoke@auratest.dev",
    handle = handle,
    inviteLink = inviteLink,
    authProvider = AuthProvider.EMAIL,
)

internal fun configOf(vararg links: Pair<ConfigSocialNetwork, String>) = AppConfig(
    socialLinks = links.map { (network, url) -> ConfigSocialLink(network, url) },
)

internal fun friendOf(
    id: String = "f1",
    name: String = "Alex K.",
    initials: String = "AK",
    spark: Long = 12_400,
    ion: Long = 1_840,
    status: FriendStatus = FriendStatus.EARNING,
) = Friend(
    id = id,
    name = name,
    initials = initials,
    spark = spark,
    ion = ion,
    status = status,
)

internal fun socialOf(
    network: SocialNetwork = SocialNetwork.DISCORD,
    webUrl: String = "https://discord.gg/ioaura",
) = SocialLink(network = network, webUrl = webUrl)

internal fun nodesState(
    handle: String = "syrex",
    invite: InviteOffer = InviteOffer(
        code = Nodes.PERSONAL_CODE,
        link = Nodes.PERSONAL_URL,
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
    var snapshot: NodesDto = nodesDto(),
    var invite: InviteStateDto = inviteDto(),
) : NodesRemoteDataSource {

    var failure: Throwable? = null

    var inviteFailure: Throwable? = null

    var calls = 0
        private set

    override suspend fun nodes(): NodesDto {
        calls++
        failure?.let { throw it }
        return snapshot
    }

    override suspend fun inviteState(): InviteStateDto {
        inviteFailure?.let { throw it }
        return invite
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
