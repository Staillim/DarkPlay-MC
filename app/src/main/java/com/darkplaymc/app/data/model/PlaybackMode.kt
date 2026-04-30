package com.darkplaymc.app.data.model

enum class PlaybackMode {
    /** Repeat the current song indefinitely */
    LOOP,
    /** Play through the list in order, repeat from start at end */
    LIST,
    /** Shuffle the queue randomly */
    SHUFFLE
}
