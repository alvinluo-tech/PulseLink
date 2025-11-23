package com.alvin.pulselink.domain.repository

import com.alvin.pulselink.domain.model.LinkRequest

/**
 * 链接请求数据仓库接口
 */
interface LinkRequestRepository {
    /**
     * 创建链接请求
     */
    suspend fun createLinkRequest(request: LinkRequest): Result<LinkRequest>
    
    /**
     * 获取发送给某个创建者的待审核请求
     */
    suspend fun getPendingRequestsForCreator(creatorId: String): Result<List<LinkRequest>>
    
    /**
     * 获取某个用户发起的所有请求
     */
    suspend fun getRequestsByRequester(requesterId: String): Result<List<LinkRequest>>
    
    /**
     * 获取某个老人的所有请求
     */
    suspend fun getRequestsBySenior(seniorId: String): Result<List<LinkRequest>>
    
    /**
     * 更新请求状态
     */
    suspend fun updateRequestStatus(requestId: String, status: String): Result<Unit>
    
    /**
     * 删除请求
     */
    suspend fun deleteRequest(requestId: String): Result<Unit>
    
    /**
     * 根据ID获取请求
     */
    suspend fun getRequestById(requestId: String): Result<LinkRequest>
}
