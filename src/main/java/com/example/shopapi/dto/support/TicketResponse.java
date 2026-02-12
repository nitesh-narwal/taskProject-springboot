package com.example.shopapi.dto.support;

import com.example.shopapi.document.SupportTicket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    private String id;
    private Long userId;
    private String userEmail;
    private String userName;
    private String subject;
    private String description;
    private SupportTicket.TicketStatus status;
    private SupportTicket.TicketPriority priority;
    private SupportTicket.TicketCategory category;
    private String orderId;
    private Long assignedTo;
    private String assignedToName;
    private List<TicketMessageDto> messages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketMessageDto {
        private Long senderId;
        private String senderName;
        private String senderRole;
        private String message;
        private List<String> attachments;
        private LocalDateTime sentAt;
    }
}

