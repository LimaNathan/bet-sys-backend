package com.coticbet.domain.enums;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * All available badges with their metadata and reward amounts.
 */
@Getter
@RequiredArgsConstructor
public enum BadgeType {

    // Categoria: A Tragédia (Derrotas e Azar)
    MICK_JAGGER(
            "Pé Frio Lendário",
            "Se ele torcer pelo Brasil, a gente perde.",
            "TRAGEDY",
            new BigDecimal("50.00")),

    TITANIC(
            "Capitão do Naufrágio",
            "Foi de arrasta pra cima em tempo recorde.",
            "TRAGEDY",
            new BigDecimal("10.00")),

    VASCO(
            "O Eterno Vice",
            "Caiu, mas passa bem.",
            "TRAGEDY",
            new BigDecimal("200.00")),

    ROBIN_HOOD_REVERSO(
            "Robin Hood Reverso",
            "Tira dos pobres (você) e dá aos ricos.",
            "TRAGEDY",
            new BigDecimal("5.00")),

    // Categoria: Gestão Duvidosa (Finanças)
    JULIUS(
            "O Pai do Chris",
            "Se não comprar nada, o desconto é maior.",
            "FINANCE",
            new BigDecimal("0.50")),

    CLT_SOFRIDO(
            "Vale-Transporte",
            "O salário cai dia 5 e acaba dia 6.",
            "FINANCE",
            new BigDecimal("20.00")),

    PRIMO_RICO(
            "O Holder",
            "O segredo é não vender (e não perder).",
            "FINANCE",
            new BigDecimal("500.00")),

    // Categoria: Sorte e Caos
    MAE_DINAH(
            "Vidente do Zap",
            "As visões estão além da compreensão.",
            "LUCK",
            new BigDecimal("1000.00")),

    INIMIGO_DO_FIM(
            "Emoção Pura",
            "Deixa tudo para a última hora.",
            "LUCK",
            new BigDecimal("20.00")),

    ILUDIDO(
            "O Sonhador",
            "Era só o Flamengo empatar...",
            "LUCK",
            new BigDecimal("50.00")),

    // Categoria: Corporativo (Internas)
    PUXA_SACO(
            "Funcionário do Mês",
            "A promoção vem aí, confia.",
            "CORPORATE",
            new BigDecimal("100.00")),

    REUNIAO_EMAIL(
            "Ocioso Profissional",
            "Trabalhar atrapalha as apostas.",
            "CORPORATE",
            new BigDecimal("15.00")),

    // Meta-Game: Platina
    DONO_DA_BANCA(
            "O DONO DA BANCA",
            "Zerou o jogo. Lenda viva.",
            "PLATINUM",
            new BigDecimal("5000.00"));

    private final String title;
    private final String description;
    private final String category;
    private final BigDecimal rewardAmount;
}
