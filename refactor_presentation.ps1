# Presentation 层重组自动化脚本
# 运行前请先备份或创建 Git 分支！

$presentationPath = "app\src\main\java\com\alvin\pulselink\presentation"

Write-Host "================================" -ForegroundColor Cyan
Write-Host "PulseLink Presentation 层重组脚本" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# 确认继续
$confirmation = Read-Host "此脚本将重组 presentation 层结构。建议先创建 Git 分支。是否继续? (y/n)"
if ($confirmation -ne 'y') {
    Write-Host "操作已取消" -ForegroundColor Yellow
    exit
}

Write-Host ""
Write-Host "开始重组..." -ForegroundColor Green
Write-Host ""

# 步骤 1: 创建新目录结构
Write-Host "[1/7] 创建新目录结构..." -ForegroundColor Yellow

$newDirectories = @(
    "$presentationPath\nav",
    "$presentationPath\common\components",
    "$presentationPath\common\theme",
    "$presentationPath\auth",
    "$presentationPath\senior\home",
    "$presentationPath\senior\health",
    "$presentationPath\senior\history",
    "$presentationPath\senior\reminder",
    "$presentationPath\senior\voice",
    "$presentationPath\senior\profile",
    "$presentationPath\caregiver\dashboard",
    "$presentationPath\caregiver\chat",
    "$presentationPath\caregiver\profile",
    "$presentationPath\caregiver\settings",
    "$presentationPath\caregiver\family"
)

foreach ($dir in $newDirectories) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Host "  ✓ 创建: $dir" -ForegroundColor Gray
    }
}

Write-Host "  完成!" -ForegroundColor Green
Write-Host ""

# 步骤 2: 移动认证相关文件
Write-Host "[2/7] 移动认证模块文件..." -ForegroundColor Yellow

$authMoves = @{
    "$presentationPath\welcome\WelcomeScreen.kt" = "$presentationPath\auth\WelcomeScreen.kt"
    "$presentationPath\forgotpassword\ForgotPasswordScreen.kt" = "$presentationPath\auth\ForgotPasswordScreen.kt"
    "$presentationPath\forgotpassword\ForgotPasswordViewModel.kt" = "$presentationPath\auth\ForgotPasswordViewModel.kt"
    "$presentationPath\forgotpassword\ForgotPasswordUiState.kt" = "$presentationPath\auth\ForgotPasswordUiState.kt"
    "$presentationPath\verification\EmailVerificationScreen.kt" = "$presentationPath\auth\EmailVerificationScreen.kt"
}

foreach ($source in $authMoves.Keys) {
    $dest = $authMoves[$source]
    if (Test-Path $source) {
        Copy-Item $source $dest -Force
        Write-Host "  ✓ 复制: $(Split-Path $source -Leaf) -> auth/" -ForegroundColor Gray
    }
}

Write-Host "  完成!" -ForegroundColor Green
Write-Host ""

# 步骤 3: 移动 Senior 端文件
Write-Host "[3/7] 移动老人端文件..." -ForegroundColor Yellow

$seniorMoves = @{
    "home" = "senior\home"
    "health" = "senior\health"
    "history" = "senior\history"
}

foreach ($source in $seniorMoves.Keys) {
    $sourcePath = "$presentationPath\$source"
    $destPath = "$presentationPath\$($seniorMoves[$source])"
    
    if (Test-Path $sourcePath) {
        Get-ChildItem $sourcePath -File | ForEach-Object {
            Copy-Item $_.FullName "$destPath\$($_.Name)" -Force
            Write-Host "  ✓ 复制: $source\$($_.Name) -> $($seniorMoves[$source])/" -ForegroundColor Gray
        }
    }
}

# 特殊处理 reminder (合并 reminder 和 reminderlist)
if (Test-Path "$presentationPath\reminder") {
    Get-ChildItem "$presentationPath\reminder" -File | ForEach-Object {
        Copy-Item $_.FullName "$presentationPath\senior\reminder\$($_.Name)" -Force
        Write-Host "  ✓ 复制: reminder\$($_.Name) -> senior\reminder/" -ForegroundColor Gray
    }
}

if (Test-Path "$presentationPath\reminderlist") {
    Get-ChildItem "$presentationPath\reminderlist" -File | ForEach-Object {
        Copy-Item $_.FullName "$presentationPath\senior\reminder\$($_.Name)" -Force
        Write-Host "  ✓ 复制: reminderlist\$($_.Name) -> senior\reminder/" -ForegroundColor Gray
    }
}

# Voice assistant
if (Test-Path "$presentationPath\assistant") {
    Get-ChildItem "$presentationPath\assistant" -File | ForEach-Object {
        Copy-Item $_.FullName "$presentationPath\senior\voice\$($_.Name)" -Force
        Write-Host "  ✓ 复制: assistant\$($_.Name) -> senior\voice/" -ForegroundColor Gray
    }
}

# Profile
if (Test-Path "$presentationPath\profile") {
    Get-ChildItem "$presentationPath\profile" -File | ForEach-Object {
        Copy-Item $_.FullName "$presentationPath\senior\profile\$($_.Name)" -Force
        Write-Host "  ✓ 复制: profile\$($_.Name) -> senior\profile/" -ForegroundColor Gray
    }
}

Write-Host "  完成!" -ForegroundColor Green
Write-Host ""

# 步骤 4: 重组 Caregiver 端文件
Write-Host "[4/7] 重组子女端文件..." -ForegroundColor Yellow

$caregiverMoves = @{
    "$presentationPath\caregiver\CareDashboardScreen.kt" = "$presentationPath\caregiver\dashboard\CareDashboardScreen.kt"
    "$presentationPath\caregiver\CareDashboardViewModel.kt" = "$presentationPath\caregiver\dashboard\CareDashboardViewModel.kt"
    "$presentationPath\caregiver\CareChatScreen.kt" = "$presentationPath\caregiver\chat\CareChatScreen.kt"
    "$presentationPath\caregiver\CaregiverProfileScreen.kt" = "$presentationPath\caregiver\profile\CaregiverProfileScreen.kt"
    "$presentationPath\caregiver\CaregiverProfileViewModel.kt" = "$presentationPath\caregiver\profile\CaregiverProfileViewModel.kt"
}

foreach ($source in $caregiverMoves.Keys) {
    $dest = $caregiverMoves[$source]
    if (Test-Path $source) {
        Copy-Item $source $dest -Force
        Write-Host "  ✓ 移动: $(Split-Path $source -Leaf)" -ForegroundColor Gray
    }
}

Write-Host "  完成!" -ForegroundColor Green
Write-Host ""

# 步骤 5: 创建导航文件 (已通过 AI 创建)
Write-Host "[5/7] 导航文件已创建" -ForegroundColor Green
Write-Host ""

# 步骤 6: 提示手动操作
Write-Host "[6/7] 需要手动操作的项目:" -ForegroundColor Yellow
Write-Host "  1. 更新所有移动文件的 package 声明" -ForegroundColor Cyan
Write-Host "  2. 更新 import 语句" -ForegroundColor Cyan
Write-Host "  3. 修改 LoginScreen.kt 和 RegisterScreen.kt 接收 role 参数" -ForegroundColor Cyan
Write-Host "  4. 创建 SeniorMainScreen.kt 和 CaregiverMainScreen.kt" -ForegroundColor Cyan
Write-Host "  5. 创建 AppNavigation.kt" -ForegroundColor Cyan
Write-Host ""

# 步骤 7: 生成报告
Write-Host "[7/7] 生成重组报告..." -ForegroundColor Yellow

$reportPath = "REFACTORING_REPORT.txt"
$report = @"
Presentation 层重组报告
生成时间: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

===== 新目录结构 =====
$(Get-ChildItem $presentationPath -Directory -Recurse | Where-Object { $_.FullName -notmatch "\\build\\" } | ForEach-Object { $_.FullName.Replace((Get-Location).Path + "\", "") })

===== 待完成任务 =====
1. [ ] 更新 package 声明
2. [ ] 更新 import 语句
3. [ ] 修改 Login/Register Screen 接收 role 参数
4. [ ] 创建 SeniorMainScreen.kt
5. [ ] 创建 CaregiverMainScreen.kt
6. [ ] 创建完整的 AppNavigation.kt
7. [ ] 删除旧目录
8. [ ] 测试编译
9. [ ] 测试运行

===== 下一步 =====
请参考 REFACTORING_GUIDE.md 完成剩余步骤
"@

$report | Out-File $reportPath -Encoding UTF8
Write-Host "  报告已保存到: $reportPath" -ForegroundColor Gray
Write-Host "  完成!" -ForegroundColor Green
Write-Host ""

Write-Host "================================" -ForegroundColor Cyan
Write-Host "重组完成!" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "下一步:" -ForegroundColor Yellow
Write-Host "1. 查看 REFACTORING_GUIDE.md 了解详细步骤" -ForegroundColor White
Write-Host "2. 使用 IDE 的重构功能更新 package 和 import" -ForegroundColor White
Write-Host "3. 逐步完成剩余的手动任务" -ForegroundColor White
Write-Host "4. 测试编译和运行" -ForegroundColor White
Write-Host ""
