# 屏净 · 用 Windows 本地 Android SDK 构建调试 APK
# 在已安装 Android Studio、JDK 17+、SDK 35 的电脑上运行。
# 若尚未下载 Gradle 8.13，本脚本将联网获取官方发行压缩包。
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $MyInvocation.MyCommand.Path
$localGradle = Join-Path $project '.build-tools\gradle-8.13\bin\gradle.bat'
$sdkCandidates = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, "$env:LOCALAPPDATA\Android\Sdk")
$sdk = $sdkCandidates | Where-Object { $_ -and (Test-Path (Join-Path $_ 'platforms\android-35\android.jar')) } | Select-Object -First 1
if (-not $sdk) {
    Write-Host '错误：没有找到 Android SDK Platform 35。请在 Android Studio > SDK Manager 安装 Android API 35。' -ForegroundColor Red
    exit 1
}
$env:ANDROID_HOME = $sdk
if (-not $env:JAVA_HOME) {
    $androidStudioJbr = 'C:\Program Files\Android\Android Studio\jbr'
    if (Test-Path "$androidStudioJbr\bin\java.exe") { $env:JAVA_HOME = $androidStudioJbr }
}
if (-not (Test-Path $localGradle)) {
    $toolsDir = Join-Path $project '.build-tools'
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $downloadPath = Join-Path $toolsDir 'gradle-8.13-bin.zip'
    Write-Host '下载 Gradle 8.13 官方发行包（需要联网）...'
    Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.13-bin.zip' -OutFile $downloadPath
    Expand-Archive -Path $downloadPath -DestinationPath $toolsDir -Force
    Remove-Item -Force $downloadPath
}
Write-Host '构建屏净 v0.1.0 调试 APK ...'
Push-Location $project
try {
    & $localGradle --no-daemon assembleDebug
    if ($LASTEXITCODE -ne 0) { throw 'Gradle 构建失败。请检查网络、JDK 和 SDK。' }
    $output = Join-Path $project 'app\build\outputs\apk\debug\app-debug.apk'
    Write-Host "构建完成：$output" -ForegroundColor Green
} finally { Pop-Location }
