# Criar atalho na área de trabalho para CoticBet (MongoDB + Backend + Frontend)

$WshShell = New-Object -ComObject WScript.Shell
$desktopPath = [Environment]::GetFolderPath("Desktop")
$shortcutPath = Join-Path $desktopPath "CoticBet.lnk"

$shortcut = $WshShell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = Join-Path $PSScriptRoot "CoticBet.vbs"
$shortcut.WorkingDirectory = $PSScriptRoot
$shortcut.Description = "Iniciar CoticBet (MongoDB + Backend + Frontend)"
$shortcut.IconLocation = "C:\Windows\System32\imageres.dll,1"
$shortcut.Save()

Write-Host "Atalho criado na área de trabalho: $shortcutPath" -ForegroundColor Green


