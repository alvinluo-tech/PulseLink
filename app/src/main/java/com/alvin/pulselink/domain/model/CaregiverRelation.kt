package com.alvin.pulselink.domain.model

/**
 * 护理者与老人的关系数据模型
 * 
 * 方案C重构：独立集合管理关系
 * Collection: caregiver_relations/{relationId}
 * 
 * relationId 格式: "${caregiverId}_${seniorProfileId}"
 * 这样可以确保每对护理者-老人关系唯一，且支持高效查询
 */
data class CaregiverRelation(
    val id: String = "",                    // 关系ID: "${caregiverId}_${seniorProfileId}"
    val caregiverId: String = "",           // 护理者 UID (indexed)
    val seniorProfileId: String = "",       // 老人档案 ID (indexed)
    
    // 关系信息
    val relationship: String = "",          // 护理者是老人的什么 (例如: "Son", "Daughter")
    val nickname: String = "",              // 护理者对老人的称呼 (例如: "Dad", "Mom")
    
    // 状态管理
    val status: String = STATUS_PENDING,    // pending/active/rejected
    val createdAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null,           // 审批时间
    val approvedBy: String? = null,         // 审批人 UID
    val rejectedAt: Long? = null,           // 拒绝时间
    val rejectedBy: String? = null,         // 拒绝人 UID
    val message: String = "",               // 申请消息
    
    // 权限控制（扁平化设计，便于查询和规则验证）
    val canViewHealthData: Boolean = true,
    val canEditHealthData: Boolean = false,
    val canViewReminders: Boolean = true,
    val canEditReminders: Boolean = true,
    val canApproveRequests: Boolean = false
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACTIVE = "active"
        const val STATUS_REJECTED = "rejected"
        
        /**
         * 生成关系ID
         */
        fun generateId(caregiverId: String, seniorProfileId: String): String {
            return "${caregiverId}_${seniorProfileId}"
        }
    }
    
    /**
     * 判断关系是否已激活
     */
    val isActive: Boolean
        get() = status == STATUS_ACTIVE
    
    /**
     * 判断关系是否待审核
     */
    val isPending: Boolean
        get() = status == STATUS_PENDING
    
    /**
     * 判断关系是否已拒绝
     */
    val isRejected: Boolean
        get() = status == STATUS_REJECTED
    
    /**
     * 转换为权限对象（兼容旧代码）
     */
    fun toPermissions(): CaregiverPermissions {
        return CaregiverPermissions(
            canViewHealthData = canViewHealthData,
            canViewReminders = canViewReminders,
            canEditReminders = canEditReminders,
            canApproveLinkRequests = canApproveRequests
        )
    }
}
