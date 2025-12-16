# Scripts de Desenvolvimento - Backend

Este diretório contém scripts auxiliares para desenvolvimento, build e configuração do backend CoticBet.

## Scripts Disponíveis

### `build.ps1`
Script para build e deploy da imagem Docker do backend.

**Uso:**
```powershell
.\scripts\build.ps1
```

### `configure-firewall.ps1`
Configura o Windows Firewall para permitir acesso externo às portas da aplicação.

**Uso (como Administrador):**
```powershell
.\scripts\configure-firewall.ps1
```

**O que faz:**
- Habilita ICMP (ping)
- Abre porta 8080 (backend principal)
- Abre porta 8081 (backend alternativa)
- Abre porta 3000 (frontend)

### `configure-port-proxy.ps1`
Configura Port Proxy do Windows para resolver problemas de roteamento Docker/WSL2 em redes corporativas.

**Uso (como Administrador):**
```powershell
.\scripts\configure-port-proxy.ps1
```

**O que faz:**
- Cria regras de proxy de porta para redirecionar tráfego da interface física para a rede virtual do Docker
- Soluciona problemas de acesso externo quando Docker usa WSL2/Hyper-V

## Notas

- Scripts marcados com **(como Administrador)** requerem privilégios elevados
- Execute o PowerShell como Administrador antes de executar esses scripts
