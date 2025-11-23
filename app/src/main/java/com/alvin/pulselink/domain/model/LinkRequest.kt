package com.alvin.pulselink.domain.model

/**
 * 链接请求数据模型
 * 存储在独立的 linkRequests collection 中
 */
data class LinkRequest(
    val id: String = "", // 请求ID (自动生成)
    val seniorId: String = "", // 老人账户ID
    val requesterId: String = "", // 发起请求的护理者ID
    val creatorId: String = "", // 老人账户创建者ID
    val relationship: String = "", // 关系 (例如: "Son", "Daughter")
    val nickname: String = "", // 昵称 (例如: "小明")
    val message: String = "", // 请求消息
    val status: String = "pending", // 状态: "pending", "approved", "rejected"
    val createdAt: Long = System.currentTimeMillis(), // 请求创建时间
    val updatedAt: Long = System.currentTimeMillis() // 最后更新时间
)
