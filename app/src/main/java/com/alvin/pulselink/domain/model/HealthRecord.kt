package com.alvin.pulselink.domain.model

/**
 * 健康记录数据模型
 * 
 * 方案C重构：独立集合管理健康数据
 * Collection: health_records/{recordId}
 * 
 * 支持多种类型的健康数据，使用 type 字段区分
 */
data class HealthRecord(
    val id: String = "",                            // 记录ID (自动生成)
    val seniorProfileId: String = "",               // 老人档案 ID (indexed)
    val type: String = TYPE_BLOOD_PRESSURE,         // 记录类型
    val recordedAt: Long = System.currentTimeMillis(),
    val recordedBy: String = "",                    // 记录者 UID（老人自己或护理者）
    
    // 血压数据
    val systolic: Int? = null,                      // 收缩压
    val diastolic: Int? = null,                     // 舒张压
    
    // 心率数据
    val heartRate: Int? = null,                     // 心率 (bpm)
    
    // 血糖数据
    val bloodSugar: Double? = null,                 // 血糖 (mmol/L)
    
    // 体重数据
    val weight: Double? = null,                     // 体重 (kg)
    
    // 备注
    val notes: String = ""
) {
    companion object {
        const val TYPE_BLOOD_PRESSURE = "blood_pressure"
        const val TYPE_HEART_RATE = "heart_rate"
        const val TYPE_BLOOD_SUGAR = "blood_sugar"
        const val TYPE_WEIGHT = "weight"
        
        /**
         * 创建血压记录
         */
        fun bloodPressure(
            seniorProfileId: String,
            systolic: Int,
            diastolic: Int,
            recordedBy: String,
            notes: String = ""
        ): HealthRecord {
            return HealthRecord(
                seniorProfileId = seniorProfileId,
                type = TYPE_BLOOD_PRESSURE,
                systolic = systolic,
                diastolic = diastolic,
                recordedBy = recordedBy,
                notes = notes
            )
        }
        
        /**
         * 创建心率记录
         */
        fun heartRate(
            seniorProfileId: String,
            heartRate: Int,
            recordedBy: String,
            notes: String = ""
        ): HealthRecord {
            return HealthRecord(
                seniorProfileId = seniorProfileId,
                type = TYPE_HEART_RATE,
                heartRate = heartRate,
                recordedBy = recordedBy,
                notes = notes
            )
        }
        
        /**
         * 创建血糖记录
         */
        fun bloodSugar(
            seniorProfileId: String,
            bloodSugar: Double,
            recordedBy: String,
            notes: String = ""
        ): HealthRecord {
            return HealthRecord(
                seniorProfileId = seniorProfileId,
                type = TYPE_BLOOD_SUGAR,
                bloodSugar = bloodSugar,
                recordedBy = recordedBy,
                notes = notes
            )
        }
    }
    
    /**
     * 格式化血压显示
     */
    fun formatBloodPressure(): String {
        return if (systolic != null && diastolic != null) {
            "$systolic/$diastolic mmHg"
        } else {
            "--/-- mmHg"
        }
    }
    
    /**
     * 格式化心率显示
     */
    fun formatHeartRate(): String {
        return heartRate?.let { "$it bpm" } ?: "-- bpm"
    }
    
    /**
     * 格式化血糖显示
     */
    fun formatBloodSugar(): String {
        return bloodSugar?.let { String.format("%.1f mmol/L", it) } ?: "-- mmol/L"
    }
}

/**
 * 老人健康摘要（聚合最新数据）
 * 用于在 Dashboard 显示
 */
data class HealthSummary(
    val seniorProfileId: String = "",
    val latestBloodPressure: HealthRecord? = null,
    val latestHeartRate: HealthRecord? = null,
    val latestBloodSugar: HealthRecord? = null,
    
    // 医疗信息（从 SeniorProfile 迁移，或单独存储）
    val medicalConditions: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
    val allergies: List<String> = emptyList()
) {
    /**
     * 获取最新收缩压
     */
    val latestSystolic: Int?
        get() = latestBloodPressure?.systolic

    /**
     * 获取最新舒张压
     */
    val latestDiastolic: Int?
        get() = latestBloodPressure?.diastolic

    /**
     * 获取最新心率
     */
    val latestHeartRateValue: Int?
        get() = latestHeartRate?.heartRate ?: latestBloodPressure?.heartRate
}
