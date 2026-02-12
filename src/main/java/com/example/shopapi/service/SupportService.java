package com.example.shopapi.service;
import com.example.shopapi.document.SupportTicket;
import com.example.shopapi.dto.support.AddTicketMessageRequest;
import com.example.shopapi.dto.support.CreateTicketRequest;
import com.example.shopapi.dto.support.TicketResponse;
import com.example.shopapi.entity.User;
import com.example.shopapi.exception.ResourceNotFoundException;
import com.example.shopapi.repository.jpa.UserRepository;
import com.example.shopapi.repository.mongo.SupportTicketRepository;
import com.example.shopapi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class SupportService {
    private final SupportTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    public TicketResponse createTicket(CreateTicketRequest request) {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        SupportTicket ticket = SupportTicket.builder()
                .userId(userId).userEmail(user.getEmail()).userName(user.getFullName())
                .subject(request.getSubject()).description(request.getDescription())
                .category(request.getCategory() != null ? request.getCategory() : SupportTicket.TicketCategory.GENERAL)
                .priority(request.getPriority() != null ? request.getPriority() : SupportTicket.TicketPriority.MEDIUM)
                .orderId(request.getOrderId()).status(SupportTicket.TicketStatus.OPEN).build();
        ticket = ticketRepository.save(ticket);
        auditService.logAction("TICKET_CREATED", "SUPPORT_TICKET", ticket.getId(), "Ticket created");
        return mapToResponse(ticket);
    }
    public TicketResponse getTicketById(String ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", "id", ticketId));
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();
        if ("ROLE_USER".equals(role) && !ticket.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("SupportTicket", "id", ticketId);
        }
        return mapToResponse(ticket);
    }
    public Page<TicketResponse> getMyTickets(Pageable pageable) {
        return ticketRepository.findByUserId(getCurrentUserId(), pageable).map(this::mapToResponse);
    }
    public Page<TicketResponse> getAllTickets(Pageable pageable) {
        return ticketRepository.findAll(pageable).map(this::mapToResponse);
    }
    public Page<TicketResponse> getOpenTickets(Pageable pageable) {
        return ticketRepository.findOpenTickets(pageable).map(this::mapToResponse);
    }
    public Page<TicketResponse> getUnassignedTickets(Pageable pageable) {
        return ticketRepository.findUnassignedTickets(pageable).map(this::mapToResponse);
    }
    public Page<TicketResponse> getMyAssignedTickets(Pageable pageable) {
        return ticketRepository.findActiveTicketsByWorker(getCurrentUserId(), pageable).map(this::mapToResponse);
    }
    public TicketResponse assignTicket(String ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", "id", ticketId));
        Long workerId = getCurrentUserId();
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", workerId));
        ticket.setAssignedTo(workerId);
        ticket.setAssignedToName(worker.getFullName());
        ticket.setStatus(SupportTicket.TicketStatus.IN_PROGRESS);
        ticket = ticketRepository.save(ticket);
        auditService.logAction("TICKET_ASSIGNED", "SUPPORT_TICKET", ticket.getId(), "Assigned to " + worker.getUsername());
        return mapToResponse(ticket);
    }
    public TicketResponse addMessage(String ticketId, AddTicketMessageRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", "id", ticketId));
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if ("ROLE_USER".equals(role) && !ticket.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("SupportTicket", "id", ticketId);
        }
        SupportTicket.TicketMessage message = SupportTicket.TicketMessage.builder()
                .senderId(userId).senderName(user.getFullName()).senderRole(role)
                .message(request.getMessage()).attachments(request.getAttachments())
                .sentAt(LocalDateTime.now())
                .isInternal(Boolean.TRUE.equals(request.getIsInternal()) && !"ROLE_USER".equals(role)).build();
        ticket.addMessage(message);
        ticket.setStatus("ROLE_USER".equals(role) ? SupportTicket.TicketStatus.OPEN : SupportTicket.TicketStatus.WAITING_CUSTOMER);
        return mapToResponse(ticketRepository.save(ticket));
    }
    public TicketResponse updateTicketStatus(String ticketId, SupportTicket.TicketStatus status) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", "id", ticketId));
        ticket.setStatus(status);
        if (status == SupportTicket.TicketStatus.RESOLVED) ticket.setResolvedAt(LocalDateTime.now());
        else if (status == SupportTicket.TicketStatus.CLOSED) ticket.setClosedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);
        auditService.logAction("TICKET_STATUS_UPDATED", "SUPPORT_TICKET", ticket.getId(), "Status: " + status);
        return mapToResponse(ticket);
    }
    private TicketResponse mapToResponse(SupportTicket t) {
        String currentRole = getCurrentUserRole();
        return TicketResponse.builder().id(t.getId()).userId(t.getUserId()).userEmail(t.getUserEmail())
                .userName(t.getUserName()).subject(t.getSubject()).description(t.getDescription())
                .status(t.getStatus()).priority(t.getPriority()).category(t.getCategory())
                .orderId(t.getOrderId()).assignedTo(t.getAssignedTo()).assignedToName(t.getAssignedToName())
                .messages(t.getMessages() != null ? t.getMessages().stream()
                        .filter(m -> !Boolean.TRUE.equals(m.getIsInternal()) || !"ROLE_USER".equals(currentRole))
                        .map(m -> TicketResponse.TicketMessageDto.builder().senderId(m.getSenderId())
                                .senderName(m.getSenderName()).senderRole(m.getSenderRole())
                                .message(m.getMessage()).attachments(m.getAttachments()).sentAt(m.getSentAt()).build())
                        .collect(Collectors.toList()) : null)
                .createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
                .resolvedAt(t.getResolvedAt()).closedAt(t.getClosedAt()).build();
    }
    private Long getCurrentUserId() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }
    private String getCurrentUserRole() {
        return ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getRole();
    }
}
