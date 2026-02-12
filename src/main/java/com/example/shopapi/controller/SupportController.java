package com.example.shopapi.controller;

import com.example.shopapi.document.SupportTicket;
import com.example.shopapi.dto.common.ApiResponse;
import com.example.shopapi.dto.common.PagedResponse;
import com.example.shopapi.dto.support.AddTicketMessageRequest;
import com.example.shopapi.dto.support.CreateTicketRequest;
import com.example.shopapi.dto.support.TicketResponse;
import com.example.shopapi.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Tag(name = "Support", description = "Support ticket management endpoints")
public class SupportController {

    private final SupportService supportService;

    @PostMapping("/tickets")
    @Operation(summary = "Create a new support ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse ticket = supportService.createTicket(request);
        return ResponseEntity.ok(ApiResponse.success("Ticket created", ticket));
    }

    @GetMapping("/tickets/{id}")
    @Operation(summary = "Get ticket by ID")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(@PathVariable String id) {
        TicketResponse ticket = supportService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @GetMapping("/tickets/my-tickets")
    @Operation(summary = "Get current user's tickets")
    public ResponseEntity<PagedResponse<TicketResponse>> getMyTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<TicketResponse> tickets = supportService.getMyTickets(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PagedResponse.of(tickets.getContent(), page, size, tickets.getTotalElements()));
    }

    @PostMapping("/tickets/{id}/messages")
    @Operation(summary = "Add a message to a ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> addMessage(
            @PathVariable String id, @Valid @RequestBody AddTicketMessageRequest request) {
        TicketResponse ticket = supportService.addMessage(id, request);
        return ResponseEntity.ok(ApiResponse.success("Message added", ticket));
    }

    // Worker/Admin endpoints
    @GetMapping("/tickets")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @Operation(summary = "Get all tickets (Worker/Admin only)")
    public ResponseEntity<PagedResponse<TicketResponse>> getAllTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<TicketResponse> tickets = supportService.getAllTickets(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PagedResponse.of(tickets.getContent(), page, size, tickets.getTotalElements()));
    }

    @GetMapping("/tickets/open")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @Operation(summary = "Get open tickets (Worker/Admin only)")
    public ResponseEntity<PagedResponse<TicketResponse>> getOpenTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<TicketResponse> tickets = supportService.getOpenTickets(PageRequest.of(page, size));
        return ResponseEntity.ok(PagedResponse.of(tickets.getContent(), page, size, tickets.getTotalElements()));
    }

    @GetMapping("/tickets/unassigned")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @Operation(summary = "Get unassigned tickets (Worker/Admin only)")
    public ResponseEntity<PagedResponse<TicketResponse>> getUnassignedTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<TicketResponse> tickets = supportService.getUnassignedTickets(PageRequest.of(page, size));
        return ResponseEntity.ok(PagedResponse.of(tickets.getContent(), page, size, tickets.getTotalElements()));
    }

    @GetMapping("/tickets/assigned-to-me")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @Operation(summary = "Get tickets assigned to current worker")
    public ResponseEntity<PagedResponse<TicketResponse>> getMyAssignedTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<TicketResponse> tickets = supportService.getMyAssignedTickets(PageRequest.of(page, size));
        return ResponseEntity.ok(PagedResponse.of(tickets.getContent(), page, size, tickets.getTotalElements()));
    }

    @PostMapping("/tickets/{id}/assign")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @Operation(summary = "Assign ticket to current worker")
    public ResponseEntity<ApiResponse<TicketResponse>> assignTicket(@PathVariable String id) {
        TicketResponse ticket = supportService.assignTicket(id);
        return ResponseEntity.ok(ApiResponse.success("Ticket assigned", ticket));
    }

    @PatchMapping("/tickets/{id}/status")
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @Operation(summary = "Update ticket status (Worker/Admin only)")
    public ResponseEntity<ApiResponse<TicketResponse>> updateStatus(
            @PathVariable String id, @RequestParam SupportTicket.TicketStatus status) {
        TicketResponse ticket = supportService.updateTicketStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Status updated", ticket));
    }
}

