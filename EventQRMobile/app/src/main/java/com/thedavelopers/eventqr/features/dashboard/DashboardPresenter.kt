package com.thedavelopers.eventqr.features.dashboard

import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.core.api.dto.RegistrationStatus
import com.thedavelopers.eventqr.core.session.SessionManager
import com.thedavelopers.eventqr.features.attendee.AttendeeRepository
import com.thedavelopers.eventqr.features.dashboard.model.dto.DashboardUpcomingEvent
import com.thedavelopers.eventqr.features.events.model.dto.AttendeeEventResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.Instant

class DashboardPresenter(
    private var view: DashboardContract.View?,
    private val repository: DashboardRepository,
    private val attendeeRepository: AttendeeRepository,
    private val sessionManager: SessionManager,
) {
    private var dashboardJob: Job? = null

    fun attach(view: DashboardContract.View) {
        this.view = view
    }

    fun detach() {
        dashboardJob?.cancel()
        view = null
    }

    fun loadDashboard() {
        view?.updateHeader(sessionManager.getUserRole(), sessionManager.getFullName())
        view?.showLoading(true)
        dashboardJob = MainScope().launch {
            val currentUserDeferred = async { repository.getCurrentUser() }
            val summaryDeferred = async { repository.getSummary() }
            val eventsDeferred = async { attendeeRepository.getEvents() }
            val registrationsDeferred = async { attendeeRepository.getMyRegistrations() }

            val currentUserResult = currentUserDeferred.await()
            if (currentUserResult is NetworkResult.Success) {
                val user = currentUserResult.data
                sessionManager.saveRole(user.role)
                sessionManager.updateProfile(user.fullName, user.phoneNumber, user.email)
                sessionManager.saveAvatarFileId(user.avatarFileId)
                view?.updateHeader(user.role.name, user.fullName)
            }

            val summaryResult = summaryDeferred.await()
            val eventsResult = eventsDeferred.await()
            val registrationsResult = registrationsDeferred.await()

            view?.showLoading(false)

            if (summaryResult is NetworkResult.Success) {
                val now = Instant.now()
                val events = if (eventsResult is NetworkResult.Success) eventsResult.data else emptyList()
                val registrations = if (registrationsResult is NetworkResult.Success) registrationsResult.data else emptyList()

                val registeredEventIds = registrations
                    .filter { it.status != RegistrationStatus.CANCELLED && it.status != RegistrationStatus.NO_SHOW }
                    .map { it.eventId }
                    .toSet()

                val mappedEvents = events
                    .filter { it.eventEndAt?.isBefore(now) != true }
                    .sortedWith(compareBy<AttendeeEventResponse> { it.eventStartAt ?: Instant.MAX })
                    .map { event ->
                        val status = computeEventStatus(event, now)
                        DashboardUpcomingEvent(
                            eventId = event.eventId,
                            title = event.title,
                            location = event.location,
                            category = event.category,
                            eventStartAt = event.eventStartAt,
                            status = status,
                            description = event.description,
                            eventEndAt = event.eventEndAt,
                            capacity = event.capacity,
                            currentAttendeeCount = event.currentAttendeeCount,
                            isRegistered = registeredEventIds.contains(event.eventId),
                        )
                    }
                val upcomingEvents = mappedEvents.filter { it.status.equals("Upcoming", ignoreCase = true) }

                val registeredCount = if (registrationsResult is NetworkResult.Success) {
                    registeredEventIds.size
                } else {
                    summaryResult.data.totalRegistrations.toInt()
                }

                val upcomingCount = if (eventsResult is NetworkResult.Success) {
                    upcomingEvents.size
                } else {
                    summaryResult.data.totalEvents.toInt()
                }

                val completedCount = if (registrationsResult is NetworkResult.Success) {
                    registrations.count {
                        it.status != RegistrationStatus.CANCELLED &&
                            it.status != RegistrationStatus.NO_SHOW &&
                            it.eventEndAt?.isBefore(now) == true
                    }
                } else {
                    0L
                }

                val summary = summaryResult.data.copy(
                    totalRegistrations = registeredCount.toLong(),
                    totalEvents = upcomingCount.toLong(),
                    totalRewards = completedCount.toLong(),
                    upcomingEvents = upcomingEvents.take(1),
                    discoverEvents = mappedEvents.take(5),
                )
                view?.showSummary(summary)

                if (currentUserResult is NetworkResult.Error) {
                    view?.showMessage("Unable to refresh account role: ${currentUserResult.message}")
                }
                if (eventsResult is NetworkResult.Error) {
                    view?.showMessage("Unable to load events: ${eventsResult.message}")
                }
                if (registrationsResult is NetworkResult.Error) {
                    view?.showMessage("Unable to load registrations: ${registrationsResult.message}")
                }
            } else if (summaryResult is NetworkResult.Error) {
                view?.showError(summaryResult.message)
            }
        }
    }

    fun openSection(title: String, message: String) {
        view?.openSection(title, message)
    }

    fun logout() {
        sessionManager.clearSession()
    }

    private fun computeEventStatus(event: AttendeeEventResponse, now: Instant): String {
        val ended = event.eventEndAt?.isBefore(now) == true
        val upcoming = event.eventStartAt?.isAfter(now) == true
        return when {
            ended -> "Completed"
            upcoming -> "Upcoming"
            else -> "Active"
        }
    }
}
