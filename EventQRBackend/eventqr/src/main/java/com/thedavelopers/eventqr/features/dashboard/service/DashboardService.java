package com.thedavelopers.eventqr.features.dashboard.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedavelopers.eventqr.features.dashboard.model.dto.DashboardSummary;
import com.thedavelopers.eventqr.features.dashboard.model.dto.DashboardSummary.DashboardUpcomingEvent;
import com.thedavelopers.eventqr.features.events.repository.EventRepository;
import com.thedavelopers.eventqr.features.notifications.repository.NotificationRepository;
import com.thedavelopers.eventqr.features.registrations.model.entity.EventRegistration;
import com.thedavelopers.eventqr.features.registrations.repository.EventRegistrationRepository;
import com.thedavelopers.eventqr.features.rewards.repository.AttendeePointBalanceRepository;
import com.thedavelopers.eventqr.features.transactions.repository.TransactionLogRepository;
import com.thedavelopers.eventqr.features.users.model.entity.UserProfile;
import com.thedavelopers.eventqr.features.users.repository.UserProfileRepository;
import com.thedavelopers.eventqr.shared.constants.RegistrationStatus;
import com.thedavelopers.eventqr.shared.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final AttendeePointBalanceRepository attendeePointBalanceRepository;
    private final NotificationRepository notificationRepository;
    private final UserProfileRepository userProfileRepository;
    private final JdbcTemplate jdbcTemplate;

    public DashboardService(EventRepository eventRepository,
                            EventRegistrationRepository eventRegistrationRepository,
                            TransactionLogRepository transactionLogRepository,
                            AttendeePointBalanceRepository attendeePointBalanceRepository,
                            NotificationRepository notificationRepository,
                            UserProfileRepository userProfileRepository,
                            JdbcTemplate jdbcTemplate) {
        this.eventRepository = eventRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.attendeePointBalanceRepository = attendeePointBalanceRepository;
        this.notificationRepository = notificationRepository;
        this.userProfileRepository = userProfileRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardSummary summary(UUID userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Instant now = Instant.now();
        List<EventRegistration> registrations = eventRegistrationRepository.findByAttendeeUserId(userId);

        long registeredCount = registrations.stream()
                .filter(this::isRegistered)
                .count();
        long availableEventsCount = eventRepository.count();
        long pointsCount = attendeePointBalanceRepository.sumPointsByAttendeeUserId(userId);
        List<DashboardUpcomingEvent> upcomingEvents = loadUpcomingEvents(now);

        return new DashboardSummary(availableEventsCount, registeredCount, transactionLogRepository.count(),
                pointsCount, notificationRepository.count(), profile.getFullName(), upcomingEvents);
    }

    private boolean isRegistered(EventRegistration registration) {
        RegistrationStatus status = registration.getStatus();
        return status != RegistrationStatus.CANCELLED && status != RegistrationStatus.NO_SHOW;
    }

    private List<DashboardUpcomingEvent> loadUpcomingEvents(Instant now) {
        return jdbcTemplate.query("""
                select id, title, location, event_start_at
                from events
                where event_start_at > ?
                order by event_start_at asc
                limit 3
                """, (rs, rowNum) -> {
                    java.sql.Timestamp eventStartAt = rs.getTimestamp("event_start_at");
                    String title = rs.getString("title");
                    return new DashboardUpcomingEvent(
                            rs.getObject("id", UUID.class),
                            null,
                            title == null || title.isBlank() ? "Untitled event" : title,
                            rs.getString("location"),
                            eventStartAt == null ? null : eventStartAt.toInstant(),
                            "Upcoming");
                },
                java.sql.Timestamp.from(now));
    }
}
