package com.specforge.discussion.entity;

/** What triggered a notification. */
public enum NotificationType {
    /** The recipient was @-mentioned in a comment. */
    MENTION,
    /** A comment was posted to a thread the recipient already participates in. */
    REPLY
}
