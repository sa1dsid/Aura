package com.aura.feature.home.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BonusStepPageTest {

    private fun teaser(
        completed: Int,
        congratulated: Int = completed,
        signalLock: Int = 0,
        networkSync: Int = 0,
        fullUplink: Int = 0,
        dataShareGb: Int = 0,
        dataShareSoon: Boolean = true,
    ) = BonusWithdrawalTeaser(
        completedSteps = completed,
        totalSteps = BONUS_TOTAL_STEPS,
        signalLockTaps = signalLock,
        networkSyncFriends = networkSync,
        fullUplinkDays = fullUplink,
        dataShareGb = dataShareGb,
        isDataShareSoon = dataShareSoon,
        congratulatedSteps = congratulated,
    )

    @Test
    fun `a brand new account opens on the first task`() {
        val page = teaser(completed = 0).openingPage()

        assertEquals(
            BonusStepPage.Task(
                step = BonusStep.SIGNAL_LOCK,
                completedSteps = 0,
                current = 0,
                isLocked = false,
            ),
            page,
        )
    }

    @Test
    fun `the open task carries how far the account has come`() {
        val page = teaser(completed = 1, networkSync = 3).openingPage()

        assertEquals(
            BonusStepPage.Task(
                step = BonusStep.NETWORK_SYNC,
                completedSteps = 1,
                current = 3,
                isLocked = false,
            ),
            page,
        )
    }

    @Test
    fun `a step closed since the last visit greets the account first`() {
        val page = teaser(completed = 1, congratulated = 0, signalLock = 20).openingPage()

        assertEquals(BonusStepPage.Done(BonusStep.SIGNAL_LOCK, completedSteps = 1), page)
    }

    @Test
    fun `the greeting hands over to the step the server unlocked`() {
        val greeted = teaser(completed = 1, congratulated = 0, networkSync = 2)
        val acknowledged = teaser(completed = 1, congratulated = 1, networkSync = 2)

        assertTrue(greeted.hasPendingCongratulation)
        assertEquals(
            BonusStepPage.Task(
                step = BonusStep.NETWORK_SYNC,
                completedSteps = 1,
                current = 2,
                isLocked = false,
            ),
            acknowledged.openingPage(),
        )
    }

    @Test
    fun `two steps closed at once are greeted one after another`() {
        val first = teaser(completed = 2, congratulated = 0)
        val second = teaser(completed = 2, congratulated = 1)

        assertEquals(BonusStepPage.Done(BonusStep.SIGNAL_LOCK, completedSteps = 1), first.openingPage())
        assertEquals(BonusStepPage.Done(BonusStep.NETWORK_SYNC, completedSteps = 2), second.openingPage())
    }

    @Test
    fun `a server that never heard of the flag never greets twice`() {
        val page = teaser(completed = 2).openingPage()

        assertFalse(teaser(completed = 2).hasPendingCongratulation)
        assertTrue(page is BonusStepPage.Task)
    }

    @Test
    fun `traffic sharing opens locked while the release keeps it off`() {
        val page = teaser(completed = 3, dataShareGb = 2).openingPage()

        assertEquals(
            BonusStepPage.Task(
                step = BonusStep.DATA_SHARE,
                completedSteps = 3,
                current = 2,
                isLocked = true,
            ),
            page,
        )
    }

    @Test
    fun `traffic sharing unlocks once the network goes live`() {
        val page = teaser(completed = 3, dataShareGb = 2, dataShareSoon = false).openingPage()

        assertEquals(false, (page as BonusStepPage.Task).isLocked)
    }

    @Test
    fun `the last step has no greeting to hand over to`() {
        assertFalse(teaser(completed = 4, congratulated = 3).hasPendingCongratulation)
    }

    @Test
    fun `a count past the target never overflows the ring`() {
        val page = teaser(completed = 0, signalLock = 25).openingPage()

        assertEquals(SIGNAL_LOCK_TAPS, (page as BonusStepPage.Task).current)
    }

    @Test
    fun `every step carries the target the specs fixed`() {
        assertEquals(20, BonusStep.SIGNAL_LOCK.target)
        assertEquals(4, BonusStep.NETWORK_SYNC.target)
        assertEquals(10, BonusStep.FULL_UPLINK.target)
        assertEquals(5, BonusStep.DATA_SHARE.target)
    }

    @Test
    fun `the card counts itself complete only when every step is closed`() {
        assertFalse(teaser(completed = 3).isComplete)
        assertTrue(teaser(completed = 4).isComplete)
    }
}
