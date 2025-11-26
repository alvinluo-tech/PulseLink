package com.alvin.pulselink.data.repository

import android.util.Log
import com.alvin.pulselink.domain.model.LinkRequest
import com.alvin.pulselink.domain.repository.LinkRequestRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkRequestRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : LinkRequestRepository {
    
    private val linkRequestsCollection = firestore.collection("linkRequests")
    
    override suspend fun createLinkRequest(request: LinkRequest): Result<LinkRequest> {
        return try {
            val requestId = if (request.id.isEmpty()) {
                UUID.randomUUID().toString()
            } else {
                request.id
            }
            
            val requestData = hashMapOf(
                "id" to requestId,
                "seniorId" to request.seniorId,
                "requesterId" to request.requesterId,
                "creatorId" to request.creatorId,
                "relationship" to request.relationship,
                "nickname" to request.nickname,
                "message" to request.message,
                "status" to request.status,
                "createdAt" to request.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            
            linkRequestsCollection.document(requestId).set(requestData).await()
            
            Log.d("LinkRequestRepo", "Created link request: $requestId")
            Result.success(request.copy(id = requestId))
        } catch (e: Exception) {
            Log.e("LinkRequestRepo", "Failed to create link request", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getPendingRequestsForCreator(creatorId: String): Result<List<LinkRequest>> {
        return try {
            val snapshot = linkRequestsCollection
                .whereEqualTo("creatorId", creatorId)
                .whereEqualTo("status", "pending")
                .get()
                .await()
            
            val requests = snapshot.documents.mapNotNull { doc ->
                try {
                    LinkRequest(
                        id = doc.getString("id") ?: "",
                        seniorId = doc.getString("seniorId") ?: "",
                        requesterId = doc.getString("requesterId") ?: "",
                        creatorId = doc.getString("creatorId") ?: "",
                        relationship = doc.getString("relationship") ?: "",
                        nickname = doc.getString("nickname") ?: "",
                        message = doc.getString("message") ?: "",
                        status = doc.getString("status") ?: "pending",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                } catch (e: Exception) {
                    Log.e("LinkRequestRepo", "Failed to parse request", e)
                    null
                }
            }
            
            Log.d("LinkRequestRepo", "Found ${requests.size} pending requests for creator: $creatorId")
            Result.success(requests)
        } catch (e: Exception) {
            Log.e("LinkRequestRepo", "Failed to get pending requests", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getRequestsByRequester(requesterId: String): Result<List<LinkRequest>> {
        return try {
            Log.d("LinkRequestRepo", "getRequestsByRequester - Querying for requesterId: $requesterId")
            val snapshot = linkRequestsCollection
                .whereEqualTo("requesterId", requesterId)
                .get()
                .await()
            
            Log.d("LinkRequestRepo", "getRequestsByRequester - Found ${snapshot.size()} documents")
            snapshot.documents.forEach { doc ->
                Log.d("LinkRequestRepo", "  - Document ${doc.id}: status=${doc.getString("status")}, seniorId=${doc.getString("seniorId")}")
            }
            
            val requests = snapshot.documents.mapNotNull { doc ->
                try {
                    LinkRequest(
                        id = doc.getString("id") ?: "",
                        seniorId = doc.getString("seniorId") ?: "",
                        requesterId = doc.getString("requesterId") ?: "",
                        creatorId = doc.getString("creatorId") ?: "",
                        relationship = doc.getString("relationship") ?: "",
                        nickname = doc.getString("nickname") ?: "",
                        message = doc.getString("message") ?: "",
                        status = doc.getString("status") ?: "pending",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                } catch (e: Exception) {
                    Log.e("LinkRequestRepo", "Failed to parse request", e)
                    null
                }
            }
            
            Log.d("LinkRequestRepo", "Found ${requests.size} requests by requester: $requesterId")
            Result.success(requests)
        } catch (e: Exception) {
            Log.e("LinkRequestRepo", "Failed to get requests by requester", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getRequestsBySenior(seniorId: String): Result<List<LinkRequest>> {
        return try {
            val snapshot = linkRequestsCollection
                .whereEqualTo("seniorId", seniorId)
                .get()
                .await()
            
            val requests = snapshot.documents.mapNotNull { doc ->
                try {
                    LinkRequest(
                        id = doc.getString("id") ?: "",
                        seniorId = doc.getString("seniorId") ?: "",
                        requesterId = doc.getString("requesterId") ?: "",
                        creatorId = doc.getString("creatorId") ?: "",
                        relationship = doc.getString("relationship") ?: "",
                        nickname = doc.getString("nickname") ?: "",
                        message = doc.getString("message") ?: "",
                        status = doc.getString("status") ?: "pending",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    )
                } catch (e: Exception) {
                    Log.e("LinkRequestRepo", "Failed to parse request", e)
                    null
                }
            }
            
            Log.d("LinkRequestRepo", "Found ${requests.size} requests for senior: $seniorId")
            Result.success(requests)
        } catch (e: Exception) {
            Log.e("LinkRequestRepo", "Failed to get requests by senior", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateRequestStatus(
        requestId: String,
        status: String,
        approvedBy: String?,
        approvedAt: Long?,
        rejectedBy: String?,
        rejectedAt: Long?
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to status,
                "updatedAt" to System.currentTimeMillis()
            )
            
            // 添加审批记录
            if (status == "approved" && approvedBy != null) {
                updates["approvedBy"] = approvedBy
                updates["approvedAt"] = approvedAt ?: System.currentTimeMillis()
            }
            
            if (status == "rejected" && rejectedBy != null) {
                updates["rejectedBy"] = rejectedBy
                updates["rejectedAt"] = rejectedAt ?: System.currentTimeMillis()
            }
            
            linkRequestsCollection.document(requestId).update(updates).await()
            
            Log.d("LinkRequestRepo", "Updated request $requestId to status: $status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LinkRequestRepo", "Failed to update request status", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteRequest(requestId: String): Result<Unit> {
        return try {
            linkRequestsCollection.document(requestId).delete().await()
            
            Log.d("LinkRequestRepo", "Deleted request: $requestId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LinkRequestRepo", "Failed to delete request", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getRequestById(requestId: String): Result<LinkRequest> {
        return try {
            val doc = linkRequestsCollection.document(requestId).get().await()
            
            if (!doc.exists()) {
                return Result.failure(Exception("Request not found"))
            }
            
            val request = LinkRequest(
                id = doc.getString("id") ?: "",
                seniorId = doc.getString("seniorId") ?: "",
                requesterId = doc.getString("requesterId") ?: "",
                creatorId = doc.getString("creatorId") ?: "",
                relationship = doc.getString("relationship") ?: "",
                nickname = doc.getString("nickname") ?: "",
                message = doc.getString("message") ?: "",
                status = doc.getString("status") ?: "pending",
                createdAt = doc.getLong("createdAt") ?: 0L,
                updatedAt = doc.getLong("updatedAt") ?: 0L,
                approvedBy = doc.getString("approvedBy"),
                approvedAt = doc.getLong("approvedAt"),
                rejectedBy = doc.getString("rejectedBy"),
                rejectedAt = doc.getLong("rejectedAt")
            )
            
            Log.d("LinkRequestRepo", "Found request: $requestId")
            Result.success(request)
        } catch (e: Exception) {
            Log.e("LinkRequestRepo", "Failed to get request by id", e)
            Result.failure(e)
        }
    }
}
