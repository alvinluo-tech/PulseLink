package com.alvin.pulselink.domain.model

import com.alvin.pulselink.util.RelationshipHelper

/**
 * Extension functions for Senior model
 */

/**
 * Get the relationship information for a specific caregiver
 */
fun Senior.getRelationshipFor(caregiverId: String): CaregiverRelationship? {
    return caregiverRelationships[caregiverId]
}

/**
 * Get the display name for a specific caregiver
 * Returns nickname if set, otherwise returns default address title based on relationship
 */
fun Senior.getDisplayNameFor(caregiverId: String): String {
    val relationship = getRelationshipFor(caregiverId) ?: return name
    
    return if (relationship.nickname.isNotBlank()) {
        relationship.nickname
    } else {
        RelationshipHelper.getDefaultAddressTitle(relationship.relationship, gender)
    }
}

/**
 * Get the relationship string for a specific caregiver
 */
fun Senior.getRelationshipStringFor(caregiverId: String): String {
    return getRelationshipFor(caregiverId)?.relationship ?: ""
}

/**
 * Get the nickname for a specific caregiver
 */
fun Senior.getNicknameFor(caregiverId: String): String {
    return getRelationshipFor(caregiverId)?.nickname ?: ""
}

/**
 * Check if a caregiver has an active relationship with this senior
 */
fun Senior.hasActiveRelationship(caregiverId: String): Boolean {
    val relationship = getRelationshipFor(caregiverId) ?: return false
    return relationship.status == "active"
}

/**
 * Check if a caregiver has a pending relationship with this senior
 */
fun Senior.hasPendingRelationship(caregiverId: String): Boolean {
    val relationship = getRelationshipFor(caregiverId) ?: return false
    return relationship.status == "pending"
}
