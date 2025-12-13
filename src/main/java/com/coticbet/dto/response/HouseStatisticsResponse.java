package com.coticbet.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseStatisticsResponse {

    private BigDecimal totalBetsReceived; // Total de apostas recebidas
    private BigDecimal totalPayouts; // Total pago aos vencedores
    private BigDecimal houseProfit; // Lucro da casa (recebido - pago)
    private BigDecimal totalInUserWallets; // Total nos wallets dos usuários
    private long totalBetsCount; // Número total de apostas
    private long pendingBetsCount; // Apostas pendentes
    private long totalUsers; // Total de usuários
    private long totalEvents; // Total de eventos
    private long openEvents; // Eventos abertos
    private long settledEvents; // Eventos liquidados
}
