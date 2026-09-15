package com.klarfinance.app.domain.repository

import com.klarfinance.app.domain.model.LocationOption

interface LocationRepository {
    suspend fun getProvinces(): Result<List<LocationOption>>
    suspend fun getRegencies(provinceId: Long): Result<List<LocationOption>>
    suspend fun getDistricts(regencyId: Long): Result<List<LocationOption>>
    suspend fun getVillages(districtId: Long): Result<List<LocationOption>>
}
