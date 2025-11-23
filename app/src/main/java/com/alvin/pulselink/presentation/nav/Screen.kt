package com.alvin.pulselink.presentation.nav

/**
 * 应用路由定义
 * 统一管理所有页面的导航路由
 * 
 * 采用 "公共模块共享 + 角色模块分流" 的策略：
 * - 认证相关页面接收 role 参数，动态改变 UI
 * - 业务功能页面按角色分流到 senior/ 和 caregiver/
 */
sealed class Screen(val route: String) {
    
    // ===== 认证相关（公共） =====
    /** 启动时的身份验证检查页 */
    object AuthCheck : Screen("auth_check")
    
    /** 欢迎页 - 角色选择入口 */
    object Welcome : Screen("welcome")
    
    /** 登录页 - 接收 role 参数 (senior/caregiver) */
    object Login : Screen("login/{role}") {
        fun createRoute(role: String) = "login/$role"
    }
    
    /** 注册页 - 接收 role 参数 */
    object Register : Screen("register/{role}") {
        fun createRoute(role: String) = "register/$role"
    }
    
    /** 忘记密码 - 接收 role 参数 */
    object ForgotPassword : Screen("forgot_password/{role}") {
        fun createRoute(role: String) = "forgot_password/$role"
    }
    
    /** 邮箱验证 */
    object EmailVerification : Screen("email_verification/{email}") {
        fun createRoute(email: String) = "email_verification/$email"
    }
    
    // ===== 老人端专属功能流 =====
    /** 老人端主页 */
    object SeniorHome : Screen("senior/home")
    
    /** 健康数据 */
    object HealthData : Screen("senior/health_data")
    
    /** 健康历史 */
    object HealthHistory : Screen("senior/health_history")
    
    /** 提醒（临近提醒反馈） */
    object Reminder : Screen("senior/reminder")
    
    /** 提醒列表（今日所有提醒） */
    object ReminderList : Screen("senior/reminder_list")
    
    /** 语音助手 */
    object VoiceAssistant : Screen("senior/voice_assistant")
    
    /** 老人个人资料 */
    object SeniorProfile : Screen("senior/profile")
    
    /** 老人端设置 */
    object SeniorSettings : Screen("senior/settings")
    
    // ===== 子女端专属功能流 =====
    /** 子女端主页（仪表盘） */
    object CaregiverHome : Screen("caregiver/home")
    
    /** 护理聊天 */
    object CareChat : Screen("caregiver/chat")
    
    /** 子女个人资料 */
    object CaregiverProfile : Screen("caregiver/profile")
    
    /** 设置 */
    object CareSettings : Screen("caregiver/settings")
    
    /** 创建老人账户（管理已创建的老人列表） */
    object ManageSeniors : Screen("caregiver/manage_seniors")
    
    /** 创建新的老人账户表单 */
    object CreateSenior : Screen("caregiver/create_senior") {
        fun createRouteForEdit(seniorId: String): String = "$route?seniorId=$seniorId"
    }
    
    /** 绑定已存在的老人账户 */
    object LinkSenior : Screen("caregiver/link_senior")
    
    /** 链接请求历史记录 */
    object LinkHistory : Screen("caregiver/link_history")
    
    /** 家庭成员链接请求审批（Link Guard） */
    object FamilyRequests : Screen("caregiver/family_requests")
    
    // ===== 测试页面 (开发用) =====
    object FirebaseTest : Screen("test/firebase")
}

/**
 * 用户角色常量
 */
object Role {
    const val SENIOR = "senior"
    const val CAREGIVER = "caregiver"
}
