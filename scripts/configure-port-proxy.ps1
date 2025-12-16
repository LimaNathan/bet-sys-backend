# ============================================
# Script de Port Proxy - Windows
# CoticBet - Resolver Problema de Acesso Docker
# ============================================
# IMPORTANTE: Execute este script como Administrador
# Clique com botão direito no PowerShell > Executar como Administrador

Write-Host "=== Configurando Port Proxy do Windows ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Este script cria regras de proxy de porta para permitir que" -ForegroundColor Yellow
Write-Host "tráfego externo alcance os containers Docker via WSL2/Hyper-V." -ForegroundColor Yellow
Write-Host ""

# Identificar o IP do WSL ou interface Docker
Write-Host "[1/3] Identificando interfaces de rede Docker/WSL..." -ForegroundColor Yellow

# Obter o IP da interface vEthernet (usado pelo Docker)
$dockerIP = (Get-NetIPAddress -InterfaceAlias "vEthernet*" | Where-Object {$_.AddressFamily -eq "IPv4" -and $_.IPAddress -like "172.*"} | Select-Object -First 1).IPAddress

if ($dockerIP) {
    Write-Host "  ✓ IP Docker/vEthernet encontrado: $dockerIP" -ForegroundColor Green
} else {
    Write-Host "  ! Não foi possível encontrar IP do Docker automaticamente" -ForegroundColor Red
    Write-Host "  Usando IP padrão do Docker Desktop: 127.0.0.1" -ForegroundColor Yellow
    $dockerIP = "127.0.0.1"
}

Write-Host ""

# Limpar regras antigas (se existirem)
Write-Host "[2/3] Removendo regras antigas (se existirem)..." -ForegroundColor Yellow
try {
    netsh interface portproxy delete v4tov4 listenport=3000 listenaddress=172.25.10.34 2>$null
    netsh interface portproxy delete v4tov4 listenport=8081 listenaddress=172.25.10.34 2>$null
    Write-Host "  ✓ Regras antigas removidas" -ForegroundColor Green
} catch {
    Write-Host "  - Nenhuma regra antiga encontrada" -ForegroundColor Gray
}

Write-Host ""

# Criar novas regras de port proxy
Write-Host "[3/3] Criando regras de Port Proxy..." -ForegroundColor Yellow

# Frontend (porta 3000)
Write-Host "  Criando proxy: 172.25.10.34:3000 -> $dockerIP:3000" -ForegroundColor Cyan
try {
    netsh interface portproxy add v4tov4 listenport=3000 listenaddress=172.25.10.34 connectport=3000 connectaddress=$dockerIP
    Write-Host "  ✓ Proxy criado para porta 3000 (Frontend)" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Erro ao criar proxy para porta 3000: $_" -ForegroundColor Red
}

# Backend (porta 8081)
Write-Host "  Criando proxy: 172.25.10.34:8081 -> $dockerIP:8081" -ForegroundColor Cyan
try {
    netsh interface portproxy add v4tov4 listenport=8081 listenaddress=172.25.10.34 connectport=8081 connectaddress=$dockerIP
    Write-Host "  ✓ Proxy criado para porta 8081 (Backend)" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Erro ao criar proxy para porta 8081: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Configuração Completa ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Verificando regras criadas:" -ForegroundColor Yellow
netsh interface portproxy show all

Write-Host ""
Write-Host "IMPORTANTE:" -ForegroundColor Green
Write-Host "1. As regras foram criadas e persistem entre reinícios" -ForegroundColor White
Write-Host "2. Peça aos seus colegas para testarem novamente:" -ForegroundColor White
Write-Host "   - http://172.25.10.34:3000 (Frontend)" -ForegroundColor Cyan
Write-Host "   - http://172.25.10.34:8081 (Backend)" -ForegroundColor Cyan
Write-Host ""
Write-Host "Para REMOVER as regras no futuro (se necessário):" -ForegroundColor Yellow
Write-Host "  netsh interface portproxy delete v4tov4 listenport=3000 listenaddress=172.25.10.34" -ForegroundColor Gray
Write-Host "  netsh interface portproxy delete v4tov4 listenport=8081 listenaddress=172.25.10.34" -ForegroundColor Gray
Write-Host ""
