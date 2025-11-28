package com.alvin.pulselink.domain.usecase

import com.alvin.pulselink.domain.repository.CaregiverRelationRepository
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

/**
 * Get Caregiver Permissions Use Case
 * 获取护理者对特定老人的权限
 */
class GetCaregiverPermissionsUseCase @Inject constructor(
    private val repository: CaregiverRelationRepository,
    private val auth: FirebaseAuth
) {
    
    data class Permissions(
        val canViewHealthData: Boolean = false,
        val canEditHealthData: Boolean = false,
        val canManageMedication: Boolean = false,
        val canReceiveAlerts: Boolean = false,
        val canApproveRequests: Boolean = false,
        val isProfileCreator: Boolean = false
    )
    
    suspend operator fun invoke(seniorId: String): Result<Permissions> {
        return try {
            val currentUserId = auth.currentUser?.uid 
                ?: return Result.success(Permissions())
            
            val relationId = "${currentUserId}_${seniorId}"
            
            repository.getRelationById(relationId)
                .mapCatching { relation ->
                    if (relation == null || relation.status != "active") {
                        Permissions()
                    } else {
                        Permissions(
                            canViewHealthData = relation.canViewHealthData,
                            canEditHealthData = relation.canEditHealthData,
                            canManageMedication = relation.canEditHealthData, // 使用 canEditHealthData 作为药物权限
                            canReceiveAlerts = relation.canViewHealthData, // 使用 canViewHealthData 作为接收提醒
                            canApproveRequests = relation.canViewHealthData,
                            isProfileCreator = false // TODO: Check from senior_profiles
                        )
                    }
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
