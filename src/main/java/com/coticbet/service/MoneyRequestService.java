package com.coticbet.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coticbet.domain.entity.MoneyRequest;
import com.coticbet.domain.entity.User;
import com.coticbet.domain.enums.RequestStatus;
import com.coticbet.domain.enums.TransactionOrigin;
import com.coticbet.dto.request.CreateMoneyRequestRequest;
import com.coticbet.dto.response.MoneyRequestResponse;
import com.coticbet.exception.BusinessException;
import com.coticbet.exception.ResourceNotFoundException;
import com.coticbet.repository.MoneyRequestRepository;
import com.coticbet.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MoneyRequestService {

    private final MoneyRequestRepository moneyRequestRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final WebSocketService webSocketService;

    @Transactional
    public MoneyRequestResponse createRequest(String userId, CreateMoneyRequestRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        MoneyRequest moneyRequest = MoneyRequest.builder()
                .userId(userId)
                .amountRequested(request.getAmount())
                .reason(request.getReason())
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        moneyRequest = moneyRequestRepository.save(moneyRequest);

        MoneyRequestResponse response = toResponse(moneyRequest, user.getEmail());

        // Notify admins via WebSocket
        webSocketService.broadcastAdminRequest(response);

        return response;
    }

    public List<MoneyRequestResponse> getPendingRequests() {
        return moneyRequestRepository.findByStatus(RequestStatus.PENDING)
                .stream()
                .map(req -> {
                    User user = userRepository.findById(req.getUserId()).orElse(null);
                    return toResponse(req, user != null ? user.getEmail() : null);
                })
                .collect(Collectors.toList());
    }

    public List<MoneyRequestResponse> getUserRequests(String userId) {
        return moneyRequestRepository.findByUserId(userId)
                .stream()
                .map(req -> toResponse(req, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public MoneyRequestResponse approveRequest(String requestId, String adminId) {
        MoneyRequest request = findById(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException("Request is not pending");
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedBy(adminId);
        request.setReviewedAt(LocalDateTime.now());
        moneyRequestRepository.save(request);

        // Credit user wallet
        walletService.credit(
                request.getUserId(),
                request.getAmountRequested(),
                TransactionOrigin.ADMIN_GIFT,
                requestId);

        // Notify the user
        webSocketService.notifyUser(
                request.getUserId(),
                "MONEY_REQUEST_APPROVED",
                String.format("Sua solicitação de R$ %.2f foi aprovada! Saldo atualizado.",
                        request.getAmountRequested()));

        // Broadcast to remove from other admins' screens
        webSocketService.broadcastAdminRequest(
                new RequestHandledEvent(requestId, "APPROVED"));

        return toResponse(request, null);
    }

    @Transactional
    public MoneyRequestResponse rejectRequest(String requestId, String adminId) {
        MoneyRequest request = findById(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException("Request is not pending");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setReviewedBy(adminId);
        request.setReviewedAt(LocalDateTime.now());
        moneyRequestRepository.save(request);

        // Notify the user
        webSocketService.notifyUser(
                request.getUserId(),
                "MONEY_REQUEST_REJECTED",
                "Sua solicitação de dinheiro foi rejeitada.");

        // Broadcast to remove from other admins' screens
        webSocketService.broadcastAdminRequest(
                new RequestHandledEvent(requestId, "REJECTED"));

        return toResponse(request, null);
    }

    private MoneyRequest findById(String id) {
        return moneyRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MoneyRequest", id));
    }

    private MoneyRequestResponse toResponse(MoneyRequest request, String userEmail) {
        return MoneyRequestResponse.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .userEmail(userEmail)
                .amountRequested(request.getAmountRequested())
                .reason(request.getReason())
                .status(request.getStatus())
                .reviewedBy(request.getReviewedBy())
                .createdAt(request.getCreatedAt())
                .reviewedAt(request.getReviewedAt())
                .build();
    }

    public record RequestHandledEvent(String requestId, String action) {
    }
}
