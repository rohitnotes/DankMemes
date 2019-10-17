package com.gelostech.dankmemes.utils

object Constants {

    // Global topic to receive app wide push notifications
    const val TOPIC_GLOBAL = "memes"

    // Broadcast receiver intent filters
    const val REGISTRATION_COMPLETE = "registrationComplete"
    const val PUSH_NOTIFICATION = "pushNotification"

    // Id to handle the notification in the notification tray
    const val NOTIFICATION_ID = 100
    const val NOTIFICATION_ID_BIG_IMAGE = 101

    // Feed types
    const val FEED_TYPE_MEME_IMAGE = 0

    // Prefs constants
    const val EMAIL = "email"
    const val USERNAME = "username"
    const val AVATAR = "avatar"
    const val LOGGED_IN = "logged_in"

    const val PIC_URL = "pic_url"
    const val MEME_ID = "meme_id"

    // Firebase
    const val MEMES = "memes"
    const val FAVORITES = "favorites"
    const val USER_FAVES = "user-favorites"
    const val NOTIFICATIONS = "notifications"
    const val USER_NOTIFS = "user-notifications"
    const val TIME = "time"
    const val LIKES_COUNT = "likesCount"
    const val COMMENTS_COUNT = "commentsCount"
    const val LIKES = "likes"
    const val FAVES = "faves"
    const val POSTER_ID = "memePosterID"
    const val METADATA = "metadata"
    const val LAST_ACTIVE = "last-active"

}