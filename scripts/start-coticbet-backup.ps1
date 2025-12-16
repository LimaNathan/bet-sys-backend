# ===========================================================
# CoticBet - System Tray Manager (Backend + Frontend)
# ===========================================================
# Gerencia MongoDB (Docker), Backend (Spring Boot) e Frontend (Next.js)
# ===========================================================

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

# Variáveis globais
$script:mongoProcess = $null
$script:backendProcess = $null
$script:frontendProcess = $null
$script:isRunning = $false

# Caminhos dos projetos - ajuste se necessário
$script:backendPath = "c:\Users\nathan.lima\Projetos\coticbet\bet-sys-backend"
$script:frontendPath = "c:\Users\nathan.lima\Projetos\coticbet\bet-sys-frontend"

$appName = "CoticBet"

# Criar ícone da bandeja
$notifyIcon = New-Object System.Windows.Forms.NotifyIcon

# Criar ícone (bolinha colorida)
function Create-Icon {
    param([string]$color)

    $bitmap = New-Object System.Drawing.Bitmap 16, 16
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)

    $brush = switch ($color) {
        "green" { [System.Drawing.Brushes]::LimeGreen }
        "yellow" { [System.Drawing.Brushes]::Yellow }
        "red" { [System.Drawing.Brushes]::Red }
        default { [System.Drawing.Brushes]::Gray }
    }

    $graphics.FillEllipse($brush, 2, 2, 12, 12)
    $graphics.Dispose()

    $icon = [System.Drawing.Icon]::FromHandle($bitmap.GetHicon())
    return $icon
}

# Iniciar MongoDB (Docker)
function Start-MongoDB {
    try {
        Write-Host "[MONGO] Iniciando MongoDB via Docker..." -ForegroundColor Cyan
        $result = docker-compose -f (Join-Path $script:backendPath "docker-compose.yml") up -d mongodb
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[MONGO] MongoDB iniciado com sucesso" -ForegroundColor Green
            return $true
        }
        return $false
    } catch {
        Write-Host "[MONGO] Erro: $_" -ForegroundColor Red
        return $false
    }
}

# Iniciar Backend (Spring Boot)
function Start-Backend {
    try {
        Write-Host "[BACKEND] Iniciando Spring Boot..." -ForegroundColor Cyan

        # Encontrar o JAR
        $jarPath = Join-Path $script:backendPath "target\*.jar"
        $jarFile = Get-ChildItem $jarPath | Select-Object -First 1

        if (-not $jarFile) {
            Write-Host "[BACKEND] JAR não encontrado em $jarPath" -ForegroundColor Red
            Write-Host "[BACKEND] Execute 'mvn clean package' primeiro!" -ForegroundColor Yellow
            return $false
        }

        Write-Host "[BACKEND] Usando JAR: $($jarFile.Name)" -ForegroundColor Cyan

        $processInfo = New-Object System.Diagnostics.ProcessStartInfo
        $processInfo.FileName = "java"
        $processInfo.Arguments = "-jar `"$($jarFile.FullName)`""
        $processInfo.WorkingDirectory = $script:backendPath
        $processInfo.UseShellExecute = $false
        $processInfo.CreateNoWindow = $true
        $processInfo.RedirectStandardOutput = $true
        $processInfo.RedirectStandardError = $true

        $script:backendProcess = [System.Diagnostics.Process]::Start($processInfo)
        Write-Host "[BACKEND] Backend iniciado - PID: $($script:backendProcess.Id)" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "[BACKEND] Erro: $_" -ForegroundColor Red
        return $false
    }
}

# Iniciar Frontend (Next.js)
function Start-Frontend {
    try {
        Write-Host "[FRONTEND] Iniciando Next.js..." -ForegroundColor Cyan

        $processInfo = New-Object System.Diagnostics.ProcessStartInfo
        $processInfo.FileName = "cmd.exe"
        $processInfo.Arguments = "/c cd /d `"$($script:frontendPath)`" && npm run dev"
        $processInfo.WorkingDirectory = $script:frontendPath
        $processInfo.UseShellExecute = $false
        $processInfo.CreateNoWindow = $true
        $processInfo.RedirectStandardOutput = $true
        $processInfo.RedirectStandardError = $true

        $script:frontendProcess = [System.Diagnostics.Process]::Start($processInfo)
        Write-Host "[FRONTEND] Frontend iniciado - PID: $($script:frontendProcess.Id)" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "[FRONTEND] Erro: $_" -ForegroundColor Red
        return $false
    }
}

# Iniciar todos os serviços
function Start-AllServices {
    if ($script:isRunning) {
        [System.Windows.Forms.MessageBox]::Show(
            "Serviços já estão rodando!",
            $appName,
            [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Information
        )
        return
    }

    $notifyIcon.Icon = Create-Icon "yellow"
    $notifyIcon.Text = "$appName - Iniciando..."
    $notifyIcon.BalloonTipTitle = $appName
    $notifyIcon.BalloonTipText = "Iniciando MongoDB, Backend e Frontend..."
    $notifyIcon.BalloonTipIcon = [System.Windows.Forms.ToolTipIcon]::Info
    $notifyIcon.ShowBalloonTip(2000)

    # Iniciar serviços em ordem
    $mongoOk = Start-MongoDB
    if (-not $mongoOk) {
        [System.Windows.Forms.MessageBox]::Show(
            "Erro ao iniciar MongoDB!",
            $appName,
            [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Error
        )
        return
    }

    Start-Sleep -Seconds 3  # Aguardar MongoDB inicializar

    $backendOk = Start-Backend
    if (-not $backendOk) {
        [System.Windows.Forms.MessageBox]::Show(
            "Erro ao iniciar Backend!",
            $appName,
            [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Error
        )
        return
    }

    Start-Sleep -Seconds 5  # Aguardar Backend inicializar

    $frontendOk = Start-Frontend
    if (-not $frontendOk) {
        [System.Windows.Forms.MessageBox]::Show(
            "Erro ao iniciar Frontend!",
            $appName,
            [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Error
        )
        return
    }

    $script:isRunning = $true
    $notifyIcon.Icon = Create-Icon "green"
    $notifyIcon.Text = "$appName - Rodando`nFrontend: http://localhost:3000`nBackend: http://localhost:8090"

    $notifyIcon.BalloonTipTitle = $appName
    $notifyIcon.BalloonTipText = "Todos os serviços iniciados!`nFrontend: http://localhost:3000"
    $notifyIcon.BalloonTipIcon = [System.Windows.Forms.ToolTipIcon]::Info
    $notifyIcon.ShowBalloonTip(3000)

    Update-ContextMenu
}

# Parar todos os serviços
function Stop-AllServices {
    if (-not $script:isRunning) {
        [System.Windows.Forms.MessageBox]::Show(
            "Serviços não estão rodando!",
            $appName,
            [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Warning
        )
        return
    }

    Write-Host "`n[STOP] Parando todos os serviços..." -ForegroundColor Yellow

    # Parar Frontend
    if ($script:frontendProcess -and -not $script:frontendProcess.HasExited) {
        Write-Host "[FRONTEND] Parando..." -ForegroundColor Yellow
        Stop-Process -Id $script:frontendProcess.Id -Force -ErrorAction SilentlyContinue
        Get-Process -Name node -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    }

    # Parar Backend
    if ($script:backendProcess -and -not $script:backendProcess.HasExited) {
        Write-Host "[BACKEND] Parando..." -ForegroundColor Yellow
        Stop-Process -Id $script:backendProcess.Id -Force -ErrorAction SilentlyContinue
        Get-Process -Name java -ErrorAction SilentlyContinue |
            Where-Object { $_.MainWindowTitle -like "*spring-boot*" } |
            Stop-Process -Force -ErrorAction SilentlyContinue
    }

    # Parar MongoDB
    Write-Host "[MONGO] Parando MongoDB..." -ForegroundColor Yellow
    docker-compose -f (Join-Path $script:backendPath "docker-compose.yml") stop mongodb | Out-Null

    $script:isRunning = $false
    $script:frontendProcess = $null
    $script:backendProcess = $null

    $notifyIcon.Icon = Create-Icon "red"
    $notifyIcon.Text = "$appName - Parado"

    $notifyIcon.BalloonTipTitle = $appName
    $notifyIcon.BalloonTipText = "Todos os serviços parados!"
    $notifyIcon.BalloonTipIcon = [System.Windows.Forms.ToolTipIcon]::Warning
    $notifyIcon.ShowBalloonTip(2000)

    Update-ContextMenu
}

# Abrir frontend no browser
function Open-Frontend {
    Start-Process "http://172.25.10.34:3000"
}

# Abrir backend no browser
function Open-Backend {
    Start-Process "http://172.25.10.34:8090/api"
}

# Sair da aplicação
function Exit-App {
    if ($script:isRunning) {
        $result = [System.Windows.Forms.MessageBox]::Show(
            "Serviços estão rodando. Deseja parar tudo e sair?",
            $appName,
            [System.Windows.Forms.MessageBoxButtons]::YesNo,
            [System.Windows.Forms.MessageBoxIcon]::Question
        )

        if ($result -eq [System.Windows.Forms.DialogResult]::Yes) {
            Stop-AllServices
            Start-Sleep -Milliseconds 1000
        } else {
            return
        }
    }

    $notifyIcon.Visible = $false
    $notifyIcon.Dispose()
    [System.Windows.Forms.Application]::Exit()
}

# Criar menu de contexto
function Update-ContextMenu {
    $contextMenu = New-Object System.Windows.Forms.ContextMenuStrip

    # Status
    $statusItem = New-Object System.Windows.Forms.ToolStripMenuItem
    $statusItem.Text = if ($script:isRunning) { "● Rodando" } else { "○ Parado" }
    $statusItem.Enabled = $false
    $contextMenu.Items.Add($statusItem) | Out-Null

    $contextMenu.Items.Add((New-Object System.Windows.Forms.ToolStripSeparator)) | Out-Null

    # Iniciar/Parar
    if ($script:isRunning) {
        $stopItem = New-Object System.Windows.Forms.ToolStripMenuItem
        $stopItem.Text = "⏹️ Parar Todos os Serviços"
        $stopItem.Add_Click({ Stop-AllServices })
        $contextMenu.Items.Add($stopItem) | Out-Null
    } else {
        $startItem = New-Object System.Windows.Forms.ToolStripMenuItem
        $startItem.Text = "▶️ Iniciar Todos os Serviços"
        $startItem.Add_Click({ Start-AllServices })
        $contextMenu.Items.Add($startItem) | Out-Null
    }

    $contextMenu.Items.Add((New-Object System.Windows.Forms.ToolStripSeparator)) | Out-Null

    # Abrir aplicações
    $openFrontendItem = New-Object System.Windows.Forms.ToolStripMenuItem
    $openFrontendItem.Text = "💻 Abrir Frontend"
    $openFrontendItem.Add_Click({ Open-Frontend })
    $contextMenu.Items.Add($openFrontendItem) | Out-Null

    $openBackendItem = New-Object System.Windows.Forms.ToolStripMenuItem
    $openBackendItem.Text = "🔧 Abrir Backend API"
    $openBackendItem.Add_Click({ Open-Backend })
    $contextMenu.Items.Add($openBackendItem) | Out-Null

    $contextMenu.Items.Add((New-Object System.Windows.Forms.ToolStripSeparator)) | Out-Null

    # Sair
    $exitItem = New-Object System.Windows.Forms.ToolStripMenuItem
    $exitItem.Text = "❌ Sair"
    $exitItem.Add_Click({ Exit-App })
    $contextMenu.Items.Add($exitItem) | Out-Null

    $notifyIcon.ContextMenuStrip = $contextMenu
}

# Configurar ícone inicial
$notifyIcon.Icon = Create-Icon "red"
$notifyIcon.Text = "$appName - Parado"
$notifyIcon.Visible = $true

# Evento de duplo clique
$notifyIcon.Add_DoubleClick({
    if ($script:isRunning) {
        Stop-AllServices
    } else {
        Start-AllServices
    }
})

# Criar menu inicial
Update-ContextMenu

# Mostrar mensagem de boas-vindas
$notifyIcon.BalloonTipTitle = $appName
$notifyIcon.BalloonTipText = "Gerenciador CoticBet iniciado!`nDuplo clique para iniciar os serviços"
$notifyIcon.BalloonTipIcon = [System.Windows.Forms.ToolTipIcon]::Info
$notifyIcon.ShowBalloonTip(3000)

# Manter aplicação rodando
[System.Windows.Forms.Application]::Run()
