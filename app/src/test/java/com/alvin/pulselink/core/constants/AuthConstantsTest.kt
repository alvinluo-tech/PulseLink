package com.alvin.pulselink.core.constants

import org.junit.Test
import org.junit.Assert.*

/**
 * AuthConstants 单元测试
 */
class AuthConstantsTest {
    
    @Test
    fun `SNR-ID 正则表达式验证 - 有效格式`() {
        // 12位字母数字组合
        assertTrue("SNR-ABCD1234EFGH".matches(AuthConstants.SNR_ID_REGEX))
        assertTrue("SNR-000000000000".matches(AuthConstants.SNR_ID_REGEX))
        assertTrue("SNR-ZZZZZZZZZZZZ".matches(AuthConstants.SNR_ID_REGEX))
        assertTrue("SNR-A1B2C3D4E5F6".matches(AuthConstants.SNR_ID_REGEX))
    }
    
    @Test
    fun `SNR-ID 正则表达式验证 - 无效格式`() {
        // 旧格式（8位）
        assertFalse("SNR-ABCD1234".matches(AuthConstants.SNR_ID_REGEX))
        
        // 长度不对
        assertFalse("SNR-ABC".matches(AuthConstants.SNR_ID_REGEX))
        assertFalse("SNR-ABCD1234EFGH123".matches(AuthConstants.SNR_ID_REGEX))
        
        // 缺少前缀
        assertFalse("ABCD1234EFGH".matches(AuthConstants.SNR_ID_REGEX))
        
        // 包含小写字母
        assertFalse("SNR-abcd1234efgh".matches(AuthConstants.SNR_ID_REGEX))
        
        // 包含特殊字符
        assertFalse("SNR-ABCD-1234EFG".matches(AuthConstants.SNR_ID_REGEX))
        assertFalse("SNR-ABCD@1234EFG".matches(AuthConstants.SNR_ID_REGEX))
        
        // 空字符串
        assertFalse("".matches(AuthConstants.SNR_ID_REGEX))
    }
    
    @Test
    fun `虚拟邮箱生成`() {
        val seniorId = "SNR-KXM2VQW7ABCD"
        val email = AuthConstants.generateVirtualEmail(seniorId)
        
        assertEquals("senior_SNR-KXM2VQW7ABCD@pulselink.app", email)
    }
    
    @Test
    fun `从虚拟邮箱提取 Senior ID - 成功`() {
        val email = "senior_SNR-KXM2VQW7ABCD@pulselink.app"
        val seniorId = AuthConstants.extractSeniorIdFromEmail(email)
        
        assertEquals("SNR-KXM2VQW7ABCD", seniorId)
    }
    
    @Test
    fun `从虚拟邮箱提取 Senior ID - 失败（格式不匹配）`() {
        // 普通邮箱
        assertNull(AuthConstants.extractSeniorIdFromEmail("user@example.com"))
        
        // 旧格式（8位）
        assertNull(AuthConstants.extractSeniorIdFromEmail("senior_SNR-ABCD1234@pulselink.app"))
        
        // 域名不匹配
        assertNull(AuthConstants.extractSeniorIdFromEmail("senior_SNR-KXM2VQW7ABCD@other.com"))
        
        // 前缀不匹配
        assertNull(AuthConstants.extractSeniorIdFromEmail("user_SNR-KXM2VQW7ABCD@pulselink.app"))
    }
    
    @Test
    fun `SNR-ID 常量定义正确`() {
        assertEquals("SNR-", AuthConstants.SNR_ID_PREFIX)
        assertEquals(12, AuthConstants.SNR_ID_CONTENT_LENGTH)
        assertEquals(16, AuthConstants.SNR_ID_FULL_LENGTH) // "SNR-" (4) + 12
        assertEquals("pulselink.app", AuthConstants.VIRTUAL_EMAIL_DOMAIN)
        assertEquals("senior_", AuthConstants.VIRTUAL_EMAIL_PREFIX)
    }
    
    @Test
    fun `注册类型常量定义正确`() {
        assertEquals("SELF_REGISTERED", AuthConstants.REG_TYPE_SELF)
        assertEquals("CAREGIVER_CREATED", AuthConstants.REG_TYPE_CAREGIVER)
    }
}
