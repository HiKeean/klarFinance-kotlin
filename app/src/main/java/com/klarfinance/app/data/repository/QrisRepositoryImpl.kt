package com.klarfinance.app.data.repository

import com.klarfinance.app.core.network.ApiService
import com.klarfinance.app.data.dto.QrisConfirmRequestDto
import com.klarfinance.app.data.dto.QrisConfirmResponseDto
import com.klarfinance.app.data.dto.QrisScanRequestDto
import com.klarfinance.app.data.dto.QrisScanResponseDto
import com.klarfinance.app.domain.model.QrisConfirmResult
import com.klarfinance.app.domain.model.QrisTransactionInfo
import com.klarfinance.app.domain.repository.QrisRepository
import javax.inject.Inject

class QrisRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : QrisRepository {

    override suspend fun scan(merchantCode: String): Result<QrisTransactionInfo> = runCatching {
        val response = apiService.post<QrisScanResponseDto, QrisScanRequestDto>(
            "api/v1/nasabah/qris/transactions/scan",
            QrisScanRequestDto(merchantCode = merchantCode),
        )
        val data = response.data ?: throw IllegalStateException("Unexpected response from server")
        QrisTransactionInfo(
            token = data.token ?: throw IllegalStateException("Unexpected response from server"),
            merchantName = data.merchantName ?: "",
        )
    }

    override suspend fun confirm(token: String, amount: Long): Result<QrisConfirmResult> = runCatching {
        val response = apiService.post<QrisConfirmResponseDto, QrisConfirmRequestDto>(
            "api/v1/nasabah/qris/transactions/$token/confirm",
            QrisConfirmRequestDto(amount = amount.toDouble()),
        )
        val data = response.data ?: throw IllegalStateException("Unexpected response from server")
        QrisConfirmResult(
            loanId = data.loanId ?: 0,
            merchantName = data.merchantName ?: "",
            totalAmountDue = (data.totalAmountDue ?: 0.0).toLong(),
            installmentAmount = (data.installmentAmount ?: 0.0).toLong(),
        )
    }
}
