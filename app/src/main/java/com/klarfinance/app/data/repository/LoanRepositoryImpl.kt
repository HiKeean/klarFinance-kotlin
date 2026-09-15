package com.klarfinance.app.data.repository

import com.klarfinance.app.core.network.ApiService
import com.klarfinance.app.core.network.NetworkUnavailableException
import com.klarfinance.app.data.dto.BankAccountResponseDto
import com.klarfinance.app.data.dto.LimitSummaryResponseDto
import com.klarfinance.app.data.dto.LoanHistoryItemResponseDto
import com.klarfinance.app.data.dto.LoanRequestDto
import com.klarfinance.app.data.dto.LoanResponseDto
import com.klarfinance.app.data.dto.RepaymentRequestDto
import com.klarfinance.app.data.local.CachedInstallment
import com.klarfinance.app.data.local.LoanHistoryDao
import com.klarfinance.app.data.local.LoanHistoryItemEntity
import com.klarfinance.app.domain.model.Cached
import com.klarfinance.app.domain.model.LimitSummary
import com.klarfinance.app.domain.model.LoanHistoryItem
import com.klarfinance.app.domain.model.LoanHistoryStatus
import com.klarfinance.app.domain.model.LoanHistoryType
import com.klarfinance.app.domain.model.LoanInstallment
import com.klarfinance.app.domain.model.LoanRequestResult
import com.klarfinance.app.domain.model.SavedBankAccount
import com.klarfinance.app.domain.repository.LoanRepository
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class LoanRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val loanHistoryDao: LoanHistoryDao,
    private val json: Json,
) : LoanRepository {

    override suspend fun getLimitSummary(): Result<LimitSummary> = runCatching {
        val response = apiService.get<LimitSummaryResponseDto>("api/v1/nasabah/loan/limit")
        val data = response.data ?: throw IllegalStateException("Unexpected response from server")
        LimitSummary(
            totalLimit = (data.totalLimit ?: 0.0).toLong(),
            usedLimit = (data.usedLimit ?: 0.0).toLong(),
            availableLimit = (data.availableLimit ?: 0.0).toLong(),
            qrisQuota = data.qrisQuota?.toLong(),
            qrisUsedAmount = data.qrisUsedAmount?.toLong(),
            hasPendingLoanReview = data.hasPendingLoanReview,
        )
    }

    override suspend fun getSavedBankAccounts(): Result<List<SavedBankAccount>> = runCatching {
        val response = apiService.get<List<BankAccountResponseDto>>("api/v1/nasabah/loan/bank-accounts")
        (response.data ?: emptyList()).mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            val bankCode = dto.bankCode ?: return@mapNotNull null
            val bankAccountNumber = dto.bankAccountNumber ?: return@mapNotNull null
            SavedBankAccount(id, bankCode, bankAccountNumber)
        }
    }

    override suspend fun requestLoan(
        amount: Long,
        tenorMonths: Int,
        bankAccountNumber: String,
        bankCode: String,
        pinjolApps: List<String>,
        bankApps: List<String>,
    ): Result<LoanRequestResult> = runCatching {
        val body = LoanRequestDto(
            amount = amount.toDouble(),
            tenorMonths = tenorMonths,
            bankAccountNumber = bankAccountNumber,
            bankCode = bankCode,
            pinjolApps = pinjolApps,
            bankApps = bankApps,
        )
        val response = apiService.post<LoanResponseDto, LoanRequestDto>("api/v1/nasabah/loan", body)
        val data = response.data ?: throw IllegalStateException("Unexpected response from server")
        LoanRequestResult(
            loanId = data.loanId,
            disbursedAmount = data.disbursedAmount?.toLong(),
            adminFee = data.adminFee?.toLong(),
            totalAmountDue = data.totalAmountDue?.toLong(),
            installmentAmount = data.installmentAmount?.toLong(),
            tenorMonths = data.tenorMonths,
            reviewRequired = data.reviewRequired,
            reviewRequestId = data.reviewRequestId,
            message = data.message,
        )
    }

    override suspend fun getLoanHistory(): Result<Cached<List<LoanHistoryItem>>> {
        val networkResult = runCatching {
            val response = apiService.get<List<LoanHistoryItemResponseDto>>("api/v1/nasabah/loan/history")
            (response.data ?: emptyList()).mapNotNull { toLoanHistoryItem(it) }
        }
        networkResult.onSuccess { items ->
            loanHistoryDao.replaceAll(items.map(::toEntity))
            return Result.success(Cached(items, isFromCache = false))
        }

        val error = networkResult.exceptionOrNull()
        if (error is NetworkUnavailableException) {
            val cached = loanHistoryDao.getAll()
            if (cached.isNotEmpty()) {
                return Result.success(Cached(cached.map(::fromEntity), isFromCache = true))
            }
        }
        return Result.failure(error ?: IllegalStateException("Unknown error"))
    }

    /** Doesn't go offline-first - paying requires connectivity regardless - but writes the
     * updated item back into the cache so it isn't immediately stale if the device goes
     * offline right after a successful payment. */
    override suspend fun repay(loanId: Int, amount: Long): Result<LoanHistoryItem> = runCatching {
        val body = RepaymentRequestDto(amount = amount.toDouble())
        val response = apiService.post<LoanHistoryItemResponseDto, RepaymentRequestDto>(
            "api/v1/nasabah/loan/$loanId/repayment",
            body,
        )
        val data = response.data ?: throw IllegalStateException("Unexpected response from server")
        val item = toLoanHistoryItem(data) ?: throw IllegalStateException("Unexpected response from server")
        loanHistoryDao.upsertOne(toEntity(item))
        item
    }

    private fun toEntity(item: LoanHistoryItem): LoanHistoryItemEntity = LoanHistoryItemEntity(
        loanId = item.loanId,
        type = item.type.name,
        merchantName = item.merchantName,
        requestedAmount = item.requestedAmount,
        totalAmountDue = item.totalAmountDue,
        tenorMonths = item.tenorMonths,
        status = item.status.name,
        paidInstallments = item.paidInstallments,
        totalInstallments = item.totalInstallments,
        nextDueDate = item.nextDueDate,
        nextDueAmount = item.nextDueAmount,
        createdAt = item.createdAt,
        installmentsJson = json.encodeToString(
            item.installments.map {
                CachedInstallment(it.installmentNumber, it.dueDate, it.amount, it.isPaid, it.paidAt)
            },
        ),
        cachedAtMillis = System.currentTimeMillis(),
    )

    private fun fromEntity(entity: LoanHistoryItemEntity): LoanHistoryItem = LoanHistoryItem(
        loanId = entity.loanId,
        type = LoanHistoryType.fromBackend(entity.type),
        merchantName = entity.merchantName,
        requestedAmount = entity.requestedAmount,
        totalAmountDue = entity.totalAmountDue,
        tenorMonths = entity.tenorMonths,
        status = LoanHistoryStatus.fromBackend(entity.status),
        paidInstallments = entity.paidInstallments,
        totalInstallments = entity.totalInstallments,
        nextDueDate = entity.nextDueDate,
        nextDueAmount = entity.nextDueAmount,
        createdAt = entity.createdAt,
        installments = runCatching { json.decodeFromString<List<CachedInstallment>>(entity.installmentsJson) }
            .getOrDefault(emptyList())
            .map { LoanInstallment(it.installmentNumber, it.dueDate, it.amount, it.isPaid, it.paidAt) },
    )

    /** Shared dengan getLoanHistory dan repay - keduanya balikin shape LoanHistoryItemResponseDto
     * yang sama persis (lihat LoanController#repay, sengaja balikin baris History yang sudah
     * ter-update biar Android gak perlu GET /history ulang abis bayar). */
    private fun toLoanHistoryItem(dto: LoanHistoryItemResponseDto): LoanHistoryItem? {
        val loanId = dto.loanId ?: return null
        return LoanHistoryItem(
            loanId = loanId,
            type = LoanHistoryType.fromBackend(dto.type),
            merchantName = dto.merchantName,
            requestedAmount = (dto.requestedAmount ?: 0.0).toLong(),
            totalAmountDue = (dto.totalAmountDue ?: 0.0).toLong(),
            tenorMonths = dto.tenorMonths ?: 1,
            status = LoanHistoryStatus.fromBackend(dto.status),
            paidInstallments = dto.paidInstallments ?: 0,
            totalInstallments = dto.totalInstallments ?: 0,
            nextDueDate = dto.nextDueDate,
            nextDueAmount = dto.nextDueAmount?.toLong(),
            createdAt = dto.createdAt,
            installments = dto.installments.mapNotNull { installmentDto ->
                val number = installmentDto.installmentNumber ?: return@mapNotNull null
                LoanInstallment(
                    installmentNumber = number,
                    dueDate = installmentDto.dueDate,
                    amount = (installmentDto.amount ?: 0.0).toLong(),
                    isPaid = installmentDto.status == "PAID",
                    paidAt = installmentDto.paidAt,
                )
            },
        )
    }
}
