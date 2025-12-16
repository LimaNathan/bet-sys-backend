# Launcher Unificado CoticBet

Executável que gerencia MongoDB (Docker), Backend (Spring Boot) e Frontend (Next.js) através de um único ícone na bandeja do sistema.

## 🎯 O Que Faz

**Um único clique inicia tudo:**
1. MongoDB via Docker
2. Backend Spring Boot (porta 8090)
3. Frontend Next.js (porta 3000)

## 🚀 Como Usar

### Primeira Vez

1. Na área de trabalho, duplo clique em **"CoticBet"**
2. Ícone aparece na bandeja (amarelo = iniciando)
3. Aguardar ~10 segundos
4. Ícone fica verde = tudo rodando ✅
5. Acessar http://localhost:3000

### Controles

**Ícone na bandeja:**
- 🔴 Vermelho = Parado
- 🟡 Amarelo = Iniciando
- 🟢 Verde = Rodando

**Menu (clique direito):**
- ▶ Iniciar Todos os Serviços
- ⏹ Parar Todos os Serviços
- 🌐 Abrir Frontend
- 🔧 Abrir Backend API
- ✕ Sair

**Duplo clique:** Alternar iniciar/parar

## 📋 Ordem de Inicialização

1. **MongoDB** (Docker) - 3s
2. **Backend** (Spring Boot) - 5s
3. **Frontend** (Next.js) - imediato

Total: ~10-15 segundos

## 🛑 Parar Serviços

- Clique direito → "Parar Todos os Serviços"
- OU duplo clique (quando rodando)
- OU clique direito → "Sair"

## ⚙️ Arquivos

- `CoticBet.vbs` - Launcher principal
- `start-coticbet.ps1` - Script PowerShell
- `criar-atalho-coticbet.ps1` - Cria atalho

## ✨ Vantagens

- **Um único clique** para tudo
- **Não precisa lembrar comandos** ou ordem
- **Visual claro** do status (cores)
- **Roda em background** sem janelas
- **Fácil para colegas** testarem

## 🔧 Troubleshooting

**"Erro ao iniciar MongoDB":**
- Docker Desktop está rodando?
- Execute: `docker ps`

**"Erro ao iniciar Backend":**
- Maven está instalado?
- Execute: `mvn --version`

**"Erro ao iniciar Frontend":**
- Node.js instalado?
- Execute: `npm --version`
- Rode `npm install` na pasta do frontend

## 📝 Notas

- Logs aparecem no console se executar manualmente o `.ps1`
- Para debug, execute `start-coticbet.ps1` direto no PowerShell
- MongoDB continua no Docker (dados persistem)
