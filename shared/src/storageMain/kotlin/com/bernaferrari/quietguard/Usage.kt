package com.bernaferrari.quietguard

class Usage {
    var Time: Long = 0
    var Version: Int = 0
    var Protocol: Int = 0
    var DAddr: String? = null
    var DPort: Int = 0
    var Uid: Int = 0
    var Sent: Long = 0
    var Received: Long = 0

    override fun toString(): String =
        "time=$Time v$Version p$Protocol $DAddr/$DPort uid $Uid out $Sent in $Received"
}