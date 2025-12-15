# Cotic Bet - Backend

Sistema de apostas interno com suporte a apostas simples e múltiplas (parlay).

> 🔗 **Frontend**: [bet-sys-frontend](https://github.com/LimaNathan/bet-sys-frontend)

## 🚀 Tecnologias

- **Java 21** com Spring Boot 3.4
- **MongoDB** para persistência
- **Spring WebSocket** para notificações em tempo real
- **JWT** para autenticação

## 📦 Funcionalidades

### Apostas
- ✅ Apostas simples (1 evento)
- ✅ Apostas múltiplas/parlay (2+ eventos)
- ✅ Cálculo automático de odds multiplicadas
- ✅ Validação de duplicatas de eventos
- ✅ Settlement multi-leg (qualquer derrota = aposta perdida)

### Sistema de Eventos
- ✅ Eventos esportivos via Odds API
- ✅ Eventos internos criados por admins
- ✅ Odds dinâmicas (parimutuel) ou fixas
- ✅ Notificações de novos eventos via WebSocket

### Gamificação
- ✅ Sistema de badges/conquistas
- ✅ Leaderboards (Magnata, Trader, Mão de Alface)
- ✅ Bônus diário

## 🏃 Como Executar

```bash
# Pré-requisito: MongoDB rodando localmente

# Executar aplicação
./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080`

## 📁 Estrutura

```
src/main/java/com/coticbet/
├── config/          # Configurações (Security, WebSocket, Migration)
├── controller/      # REST Controllers
├── domain/          # Entidades e Enums
├── dto/             # Request/Response DTOs
├── repository/      # MongoDB Repositories
├── service/         # Lógica de negócio
└── exception/       # Exception handlers
```

## 🔑 Endpoints Principais

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/auth/register` | Registro de usuário |
| POST | `/api/auth/login` | Login |
| GET | `/api/events` | Listar eventos abertos |
| POST | `/api/bets` | Realizar aposta (simples ou múltipla) |
| GET | `/api/bets` | Histórico de apostas |
| GET | `/api/wallet` | Saldo da carteira |
| GET | `/api/leaderboard/*` | Rankings |

## 🎰 Formato de Aposta Múltipla

```json
POST /api/bets
{
  "amount": 50.00,
  "selections": [
    { "eventId": "event1", "optionId": "opt1" },
    { "eventId": "event2", "optionId": "opt2" }
  ]
}
```

## ⚙️ Variáveis de Ambiente

```properties
MONGODB_URI=mongodb://localhost:27017/coticbet
JWT_SECRET=your-secret-key
ODDS_API_KEY=your-odds-api-key
```
