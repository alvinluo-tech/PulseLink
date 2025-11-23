package com.alvin.pulselink.util

/**
 * Relationship Helper
 * Handles conversion between relationship identity and address titles
 */
object RelationshipHelper {
    
    /**
     * Convert relationship identity to default address title
     * 将关系身份转换为默认称呼
     * 
     * @param relationship The relationship identity (e.g., "Son", "Daughter")
     * @param gender The senior's gender to provide more specific titles
     * @return Default address title (e.g., "Father", "Mother")
     */
    fun getDefaultAddressTitle(relationship: String, gender: String = ""): String {
        return when (relationship) {
            "Son", "Daughter" -> {
                when (gender.lowercase()) {
                    "male" -> "Father"
                    "female" -> "Mother"
                    else -> "Parent"
                }
            }
            "Grandson", "Granddaughter" -> {
                when (gender.lowercase()) {
                    "male" -> "Grandfather"
                    "female" -> "Grandmother"
                    else -> "Grandparent"
                }
            }
            "Spouse" -> "Spouse"
            "Sibling" -> {
                when (gender.lowercase()) {
                    "male" -> "Brother"
                    "female" -> "Sister"
                    else -> "Sibling"
                }
            }
            "Friend" -> "Friend"
            "Caregiver" -> "Patient"
            "Other" -> "Senior"
            else -> "Senior"
        }
    }
    
    /**
     * Get available relationship options
     */
    fun getRelationshipOptions(): List<String> {
        return listOf(
            "Son",
            "Daughter",
            "Grandson",
            "Granddaughter",
            "Spouse",
            "Sibling",
            "Friend",
            "Caregiver",
            "Other"
        )
    }
    
    /**
     * Get relationship display text for senior's perspective
     * 从老人的角度显示关系（用于老人端查看请求）
     * 
     * @param relationship The caregiver's relationship to senior
     * @return Text to display to senior (e.g., "Your son")
     */
    fun getRelationshipForSenior(relationship: String): String {
        return when (relationship) {
            "Son" -> "Your son"
            "Daughter" -> "Your daughter"
            "Grandson" -> "Your grandson"
            "Granddaughter" -> "Your granddaughter"
            "Spouse" -> "Your spouse"
            "Sibling" -> "Your sibling"
            "Friend" -> "Your friend"
            "Caregiver" -> "Your caregiver"
            "Other" -> "Someone"
            else -> "Someone"
        }
    }
}
