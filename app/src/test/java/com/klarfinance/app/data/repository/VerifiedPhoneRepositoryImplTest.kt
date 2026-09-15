package com.klarfinance.app.data.repository

import com.klarfinance.app.data.local.VerifiedPhoneDao
import com.klarfinance.app.data.local.VerifiedPhoneEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class VerifiedPhoneRepositoryImplTest {

    private val dao: VerifiedPhoneDao = mockk()
    private lateinit var repository: VerifiedPhoneRepositoryImpl

    private val phone = "081234567890"

    @Before
    fun setUp() {
        repository = VerifiedPhoneRepositoryImpl(dao)
    }

    @Test
    fun `markVerified upserts an entity stamped with the current time`() = runTest {
        val slot = slot<VerifiedPhoneEntity>()
        coEvery { dao.upsert(capture(slot)) } returns Unit

        val before = System.currentTimeMillis()
        repository.markVerified(phone)
        val after = System.currentTimeMillis()

        coVerify(exactly = 1) { dao.upsert(any()) }
        assertTrue(slot.captured.phone == phone)
        assertTrue(slot.captured.verifiedAtMillis in before..after)
    }

    @Test
    fun `isRecentlyVerified returns false when no record exists`() = runTest {
        coEvery { dao.findByPhone(phone) } returns null

        assertFalse(repository.isRecentlyVerified(phone))
    }

    @Test
    fun `isRecentlyVerified returns true when verified within the last 24 hours`() = runTest {
        val recentMillis = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
        coEvery { dao.findByPhone(phone) } returns VerifiedPhoneEntity(phone, recentMillis)

        assertTrue(repository.isRecentlyVerified(phone))
    }

    @Test
    fun `isRecentlyVerified returns false when verified more than 24 hours ago`() = runTest {
        val staleMillis = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25)
        coEvery { dao.findByPhone(phone) } returns VerifiedPhoneEntity(phone, staleMillis)

        assertFalse(repository.isRecentlyVerified(phone))
    }

    @Test
    fun `isRecentlyVerified returns false for a future timestamp (clock skew guard)`() = runTest {
        val futureMillis = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
        coEvery { dao.findByPhone(phone) } returns VerifiedPhoneEntity(phone, futureMillis)

        assertFalse(repository.isRecentlyVerified(phone))
    }
}
