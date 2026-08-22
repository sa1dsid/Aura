package com.aura.feature.nodes.data.remote

import com.aura.feature.nodes.data.remote.dto.FriendDto
import com.aura.feature.nodes.data.remote.dto.NodesSnapshotDto
import com.aura.feature.nodes.data.remote.dto.SocialLinkDto
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

interface NodesRemoteDataSource {
    suspend fun fetchNodes(): NodesSnapshotDto
}

@Singleton
class MockNodesRemoteDataSource @Inject constructor() : NodesRemoteDataSource {

    override suspend fun fetchNodes(): NodesSnapshotDto {
        delay(NETWORK_DELAY_MILLIS)
        return NodesSnapshotDto(
            handle = "syrex",
            inviteCode = "SYREX482",
            inviteLink = "https://ioaura.app/i/syrex",
            inviteQuote = null,
            inviteShareText = null,
            friendsJoined = 6,
            activeFriends = 2,
            tier = "CORE_NODE",
            tierSparkPercent = 15,
            tierWithdrawalPercent = 5.0,
            nextTier = "IONIC_PRIME",
            friendsToNextTier = 1,
            earnedSpark = 3_260,
            earnedIon = 890,
            friends = FRIENDS,
            socials = SOCIALS,
        )
    }

    private companion object {
        const val NETWORK_DELAY_MILLIS = 300L

        val FRIENDS = listOf(
            FriendDto("f1", "Alex K.", "alexk", 12_400, 1_840, "EARNING"),
            FriendDto("f2", "Maria T.", "mariat", 8_900, 640, "EARNING"),
            FriendDto("f3", "Daniel R.", "danr", 54_200, 0, "SPARK_ONLY"),
            FriendDto("f4", "Jenna L.", "jennal", 3_100, 0, "SPARK_ONLY"),
            FriendDto("f5", "Peter V.", "peterv", 0, 0, "INACTIVE"),
            FriendDto("f6", "Rachel S.", "rashels", 0, 0, "INACTIVE"),
        )

        val SOCIALS = listOf(
            SocialLinkDto("DISCORD", "https://discord.gg/ioaura", null),
            SocialLinkDto("TELEGRAM", "https://t.me/ioaura", "tg://resolve?domain=ioaura"),
            SocialLinkDto("X", "https://x.com/ioaura", "twitter://user?screen_name=ioaura"),
            SocialLinkDto("REDDIT", "https://reddit.com/r/ioaura", null),
            SocialLinkDto(
                "INSTAGRAM",
                "https://instagram.com/ioaura",
                "instagram://user?username=ioaura",
            ),
            SocialLinkDto("SNAPCHAT", "https://snapchat.com/add/ioaura", null),
        )
    }
}
