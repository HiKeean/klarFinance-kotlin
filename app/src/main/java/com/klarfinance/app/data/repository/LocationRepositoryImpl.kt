package com.klarfinance.app.data.repository

import com.klarfinance.app.core.network.ApiService
import com.klarfinance.app.data.dto.LocationItemDto
import com.klarfinance.app.domain.model.LocationOption
import com.klarfinance.app.domain.repository.LocationRepository
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : LocationRepository {

    override suspend fun getProvinces(): Result<List<LocationOption>> =
        fetch("api/v1/dbo/location/provinces")

    override suspend fun getRegencies(provinceId: Long): Result<List<LocationOption>> =
        fetch("api/v1/dbo/location/regencies", mapOf("provinceId" to provinceId.toString()))

    override suspend fun getDistricts(regencyId: Long): Result<List<LocationOption>> =
        fetch("api/v1/dbo/location/districts", mapOf("regenciesId" to regencyId.toString()))

    override suspend fun getVillages(districtId: Long): Result<List<LocationOption>> =
        fetch("api/v1/dbo/location/villages", mapOf("districtsId" to districtId.toString()))

    private suspend fun fetch(path: String, params: Map<String, String> = emptyMap()): Result<List<LocationOption>> =
        runCatching {
            apiService.get<List<LocationItemDto>>(path, params).data
                .orEmpty()
                .map { LocationOption(id = it.id, name = it.name) }
        }
}
