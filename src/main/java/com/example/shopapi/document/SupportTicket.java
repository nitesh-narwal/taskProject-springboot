package com.example.shopapi.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "support_tickets")
@CompoundIndex(name = "user_status_idx", def = "{'userId': 1, 'status': 1}")
@CompoundIndex(name = "status_priority_idx", def = "{'status': 1, 'priority': 1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private String userEmail;

    private String userName;

    private String subject;

    private String description;

    @Indexed
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Builder.Default
    private TicketCategory category = TicketCategory.GENERAL;

    private String orderId;

    private Long assignedTo;

    private String assignedToName;

    @Builder.Default
    private List<TicketMessage> messages = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime resolvedAt;

    private LocalDateTime closedAt;

    public enum TicketStatus {
        OPEN,
        IN_PROGRESS,
        WAITING_CUSTOMER,
        RESOLVED,
        CLOSED
    }

    public enum TicketPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }

    public enum TicketCategory {
        GENERAL,
        ORDER,
        PAYMENT,
        SHIPPING,
        PRODUCT,
        ACCOUNT,
        REFUND,
        TECHNICAL
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TicketMessage {
        private Long senderId;
        private String senderName;
        private String senderRole;
        private String message;
        private List<String> attachments;
        private LocalDateTime sentAt;
        private Boolean isInternal;
    }

    public void addMessage(TicketMessage message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
    }
}

