package com.quietinbox.navigation

import kotlinx.serialization.Serializable

@Serializable data object Onboarding
@Serializable data object Home
@Serializable data object Inbox
@Serializable data class InboxFiltered(val packageName: String? = null, val day: Int? = null)
@Serializable data class NotificationDetail(val id: Long)
@Serializable data object Settings
@Serializable data object PermissionsHealth
@Serializable data object AllowRules
@Serializable data object Schedules
@Serializable data class ScheduleEdit(val id: Long = 0L)
@Serializable data object MutedApps
@Serializable data object Storage
@Serializable data object AppLock
@Serializable data object Privacy
@Serializable data object About
