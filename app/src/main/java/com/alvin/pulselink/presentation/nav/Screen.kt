package com.alvin.pulselink.presentation.nav

/**
 * 应用路由定义
 * 统一管理所有页面的导航路由
 */
sealed class Screen(val route: String) {
    
    // ===== 认证相关 =====
    /** 欢迎页 - 角色选择 */
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
    object EmailVerification : Screen("email_verification/{email}/{role}") {
        fun createRoute(email: String, role: String) = "email_verification/$email/$role"
    }
    
    // ===== 老人端页面 =====
    /** 老人端主页 */
    object SeniorHome : Screen("senior/home")
    
    /** 健康数据 */
    object SeniorHealthData : Screen("senior/health_data")
    
    /** 健康历史 */
    object SeniorHealthHistory : Screen("senior/health_history")
    
    /** 提醒列表 */
    object SeniorReminderList : Screen("senior/reminder_list")
    
    /** 添加/编辑提醒 */
    object SeniorReminder : Screen("senior/reminder/{id?}") {
        fun createRoute(id: String? = null) = if (id != null) "senior/reminder/$id" else "senior/reminder/new"
    }
    
    /** 语音助手 */
    object SeniorVoiceAssistant : Screen("senior/voice_assistant")
    
    /** 老人个人资料 */
    object SeniorProfile : Screen("senior/profile")
    
    // ===== 子女端页面 =====
    /** 子女端仪表盘 */
    object CareDashboard : Screen("caregiver/dashboard")
    
    /** 护理聊天选择 */
    object CareChat : Screen("caregiver/chat")
    
    /** 聊天详情 */
    object CareChatDetail : Screen("caregiver/chat/{lovedOneId}") {
        fun createRoute(lovedOneId: String) = "caregiver/chat/$lovedOneId"
    }
    
    /** 亲人详情 */
    object LovedOneDetail : Screen("caregiver/loved_one/{lovedOneId}") {
        fun createRoute(lovedOneId: String) = "caregiver/loved_one/$lovedOneId"
    }
    
    /** 子女个人资料 */
    object CaregiverProfile : Screen("caregiver/profile")
    
    /** 设置 */
    object CareSettings : Screen("caregiver/settings")
    
    /** 管理家庭成员 */
    object ManageFamily : Screen("caregiver/manage_family")
    
    /** 添加家庭成员 */
    object AddFamilyMember : Screen("caregiver/add_family_member")
    
    /** 隐私与安全 */
    object PrivacySecurity : Screen("caregiver/privacy_security")
    
    /** 帮助中心 */
    object HelpCenter : Screen("caregiver/help_center")
    
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
