# ============================================
# Script de Configuração de Firewall
# CoticBet - Permitir Acesso Externo
# ============================================
# IMPORTANTE: Execute este script como Administrador
# Clique com botão direito no PowerShell > Executar como Administrador

Write-Host "=== Configurando Firewall do Windows para CoticBet ===" -ForegroundColor Cyan
Write-Host ""

# 1. Habilitar ICMP (Ping) - Essencial para testar conectividade
Write-Host "[1/4] Habilitando ICMP (Ping)..." -ForegroundColor Yellow
try {
    # Habilita ping (Echo Request) para perfil de Domínio
    netsh advfirewall firewall add rule name="ICMP Allow incoming V4 echo request (CoticBet)" protocol=icmpv4:8,any dir=in action=allow profile=domain
    Write-Host "  ✓ ICMP habilitado com sucesso!" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Erro ao habilitar ICMP: $_" -ForegroundColor Red
}

Write-Host ""

# 2. Abrir porta do Backend (8080)
Write-Host "[2/4] Abrindo porta do Backend (8080)..." -ForegroundColor Yellow
try {
    netsh advfirewall firewall add rule name="CoticBet Backend (8080)" dir=in action=allow protocol=TCP localport=8080 profile=domain
    Write-Host "  ✓ Porta 8080 aberta!" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Erro ao abrir porta 8080: $_" -ForegroundColor Red
}

Write-Host ""

# 3. Abrir porta alternativa do Backend (8081) - caso esteja usando
Write-Host "[3/4] Abrindo porta alternativa do Backend (8081)..." -ForegroundColor Yellow
try {
    netsh advfirewall firewall add rule name="CoticBet Backend (8081)" dir=in action=allow protocol=TCP localport=8081 profile=domain
    Write-Host "  ✓ Porta 8081 aberta!" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Erro ao abrir porta 8081: $_" -ForegroundColor Red
}

Write-Host ""

# 4. Abrir porta do Frontend (3000)
Write-Host "[4/4] Abrindo porta do Frontend (3000)..." -ForegroundColor Yellow
try {
    netsh advfirewall firewall add rule name="CoticBet Frontend (3000)" dir=in action=allow protocol=TCP localport=3000 profile=domain
    Write-Host "  ✓ Porta 3000 aberta!" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Erro ao abrir porta 3000: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Configuração Completa ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Verificando regras criadas:" -ForegroundColor Yellow
netsh advfirewall firewall show rule name=all | findstr "CoticBet"

# Write-Host ""
# Write-Host "PRÓXIMOS PASSOS:" -ForegroundColor Green
# Write-Host "1. Reinicie seu backend para aplicar as configurações de rede"
# Write-Host "2. Peça aos seus colegas para testarem:"
# Write-Host "   - ping 172.25.10.34"
# Write-Host "   - Acessar http://172.25.10.34:3000 (Frontend)"
# Write-Host "   - Acessar http://172.25.10.34:8080 (Backend)"
# Write-Host ""
