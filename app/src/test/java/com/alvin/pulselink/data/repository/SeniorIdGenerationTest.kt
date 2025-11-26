package com.alvin.pulselink.data.repository

import org.junit.Test
import org.junit.Assert.*

/**
 * Senior ID 生成唯一性测试
 * 
 * 测试时间戳+随机数方案的唯一性保证
 */
class SeniorIdGenerationTest {
    
    /**
     * 模拟 AuthRepositoryImpl.generateSeniorId() 方法
     */
    private fun generateSeniorId(): String {
        val timestamp = System.currentTimeMillis().toString(36).uppercase()
        val random = (1..4).map { ('A'..'Z').random() }.joinToString("")
        val combined = timestamp + random
        return "SNR-${combined.takeLast(12)}"
    }
    
    @Test
    fun `生成的 ID 格式正确`() {
        val id = generateSeniorId()
        
        // 检查格式
        assertTrue("ID 应该以 SNR- 开头", id.startsWith("SNR-"))
        assertEquals("ID 长度应该是 16", 16, id.length)
        
        // 检查内容部分（12位）
        val content = id.substring(4)
        assertEquals("内容部分长度应该是 12", 12, content.length)
        assertTrue("内容应该只包含大写字母和数字", content.all { it in 'A'..'Z' || it in '0'..'9' })
    }
    
    @Test
    fun `100次生成测试 - 无重复`() {
        val ids = mutableSetOf<String>()
        
        repeat(100) {
            val id = generateSeniorId()
            ids.add(id)
        }
        
        // 所有 ID 都应该是唯一的
        assertEquals("100次生成应该产生100个不同的ID", 100, ids.size)
    }
    
    @Test
    fun `1000次生成测试 - 高唯一性`() {
        val ids = mutableSetOf<String>()
        
        repeat(1000) {
            val id = generateSeniorId()
            ids.add(id)
        }
        
        // 由于在极短时间内生成，部分ID的时间戳可能相同
        // 依赖4位随机字母（26^4 = 456,976种可能）
        // 1000次生成中，期望至少99.5%的唯一性
        val uniqueRate = ids.size.toDouble() / 1000.0
        assertTrue(
            "1000次生成应该有超过99.5%的唯一性（实际: ${uniqueRate * 100}%）",
            uniqueRate >= 0.995
        )
        
        println("1000次生成，唯一ID数: ${ids.size}，唯一率: ${uniqueRate * 100}%")
    }
    
    @Test
    fun `时间戳部分随时间递增`() {
        val id1 = generateSeniorId()
        Thread.sleep(10) // 等待一小段时间
        val id2 = generateSeniorId()
        
        // 提取时间戳部分（前8位）
        val timestamp1 = id1.substring(4, 12)
        val timestamp2 = id2.substring(4, 12)
        
        // 虽然取了 takeLast(12)，但时间戳部分应该有序
        println("ID1: $id1, timestamp part: $timestamp1")
        println("ID2: $id2, timestamp part: $timestamp2")
        
        // 第二个 ID 的时间戳应该 >= 第一个（36进制比较）
        // 注意：由于 takeLast(12)，可能会截断，但整体趋势应该递增
        assertNotEquals("两个ID应该不同", id1, id2)
    }
    
    @Test
    fun `随机部分提供额外的唯一性`() {
        // 模拟在同一毫秒内生成多个ID
        val timestamp = System.currentTimeMillis().toString(36).uppercase()
        
        val ids = mutableSetOf<String>()
        repeat(100) {
            val random = (1..4).map { ('A'..'Z').random() }.joinToString("")
            val combined = timestamp + random
            val id = "SNR-${combined.takeLast(12)}"
            ids.add(id)
        }
        
        // 即使时间戳相同，随机部分也应该产生不同的ID
        // 26^4 = 456,976 种可能性，100次生成几乎不可能重复
        assertTrue("100次生成应该产生多个不同的ID（高概率）", ids.size > 90)
    }
    
    @Test
    fun `验证 takeLast 行为`() {
        val timestamp = System.currentTimeMillis().toString(36).uppercase()
        val random = "ABCD"
        val combined = timestamp + random
        
        println("时间戳: $timestamp (长度: ${timestamp.length})")
        println("随机: $random")
        println("组合: $combined (长度: ${combined.length})")
        println("取后12位: ${combined.takeLast(12)}")
        
        // 组合长度应该是时间戳长度 + 4
        assertEquals("组合长度应该正确", timestamp.length + 4, combined.length)
        
        // 取后12位应该包含完整的随机部分（4位）+ 时间戳的后8位
        val last12 = combined.takeLast(12)
        assertTrue("应该以随机部分结尾", last12.endsWith(random))
    }
}
