# ============================================================
# 黔能碳溯 · 后端启动脚本
# 1) 自动加载 backend\.env 中密钥（DASHSCOPE_API_KEY / AMAP_WEB_KEY 等）
# 2) 使用 Temurin17 启动 Spring Boot（系统默认 JDK 为 8 会导致启动失败）
# 用法：powershell -ExecutionPolicy Bypass -File scripts\start-backend.ps1
# ============================================================
$ROOT = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $ROOT 'backend\.env'

# --- 加载本地密钥（覆盖已存在的同名环境变量） ---
if (Test-Path $envFile) {
  Get-Content $envFile -Encoding UTF8 | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
      $parts = $line -split '=', 2
      if ($parts.Count -eq 2) {
        [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), 'Process')
      }
    }
  }
  Write-Host "[start-backend] loaded $envFile"
} else {
  Write-Host "[start-backend] backend\.env not found, starting in no-key demo mode"
}

# --- 选择 JDK17 ---
$java17 = 'D:\jdk\temurin-17\jdk-17.0.20.1+1\bin\java.exe'
if (-not (Test-Path $java17)) {
  $java17 = (Get-Command java -ErrorAction SilentlyContinue).Source
  Write-Host "[start-backend] fallback java: $java17"
}

& $java17 -jar (Join-Path $ROOT 'backend\target\qnct-backend.jar')