package com.squareup.wire

class WireLog {
    companion object {
        // 4: debug, 3: info, 2: warm, 1: error
        @JvmStatic
        var debugLogLevel = 0
    }
}
