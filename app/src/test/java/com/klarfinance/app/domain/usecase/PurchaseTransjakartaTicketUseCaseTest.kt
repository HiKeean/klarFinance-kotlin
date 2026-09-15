package com.klarfinance.app.domain.usecase

import com.klarfinance.app.domain.model.TransjakartaTicket
import com.klarfinance.app.domain.repository.TransjakartaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PurchaseTransjakartaTicketUseCaseTest {

    private val repository: TransjakartaRepository = mockk()
    private lateinit var useCase: PurchaseTransjakartaTicketUseCase

    @Before
    fun setUp() {
        useCase = PurchaseTransjakartaTicketUseCase(repository)
    }

    @Test
    fun `zero or negative quantity fails without calling repository`() = runTest {
        val result = useCase(0)

        assertTrue(result.isFailure)
        assertEquals("Jumlah tiket harus minimal 1", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { repository.purchaseTickets(any()) }
    }

    @Test
    fun `positive quantity delegates to repository`() = runTest {
        val ticket = TransjakartaTicket(
            ticketId = 1,
            ticketCode = "TJ-1",
            amount = 3_500,
            used = false,
            purchasedAt = "2026-09-10",
            loanId = 1,
            billingCycle = "2026-09",
            dueDate = "2026-10-05",
        )
        coEvery { repository.purchaseTickets(2) } returns Result.success(listOf(ticket, ticket))

        val result = useCase(2)

        assertEquals(Result.success(listOf(ticket, ticket)), result)
    }
}
