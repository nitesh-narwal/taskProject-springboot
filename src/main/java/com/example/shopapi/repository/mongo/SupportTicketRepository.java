package com.example.shopapi.repository.mongo;

import com.example.shopapi.document.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SupportTicketRepository extends MongoRepository<SupportTicket, String> {

    Page<SupportTicket> findByUserId(Long userId, Pageable pageable);

    Page<SupportTicket> findByStatus(SupportTicket.TicketStatus status, Pageable pageable);

    Page<SupportTicket> findByPriority(SupportTicket.TicketPriority priority, Pageable pageable);

    Page<SupportTicket> findByCategory(SupportTicket.TicketCategory category, Pageable pageable);

    Page<SupportTicket> findByAssignedTo(Long assignedTo, Pageable pageable);

    Page<SupportTicket> findByUserIdAndStatus(Long userId, SupportTicket.TicketStatus status, Pageable pageable);

    @Query("{'status': {$in: ['OPEN', 'IN_PROGRESS', 'WAITING_CUSTOMER']}}")
    Page<SupportTicket> findOpenTickets(Pageable pageable);

    @Query("{'assignedTo': null, 'status': 'OPEN'}")
    Page<SupportTicket> findUnassignedTickets(Pageable pageable);

    @Query("{'assignedTo': ?0, 'status': {$in: ['OPEN', 'IN_PROGRESS']}}")
    Page<SupportTicket> findActiveTicketsByWorker(Long workerId, Pageable pageable);

    long countByStatus(SupportTicket.TicketStatus status);

    long countByUserId(Long userId);

    @Query(value = "{'status': {$in: ['OPEN', 'IN_PROGRESS', 'WAITING_CUSTOMER']}}", count = true)
    long countOpenTickets();

    @Query(value = "{'createdAt': {$gte: ?0}}", count = true)
    long countTicketsSince(LocalDateTime since);

    @Query("{'orderId': ?0}")
    List<SupportTicket> findByOrderId(String orderId);
}

