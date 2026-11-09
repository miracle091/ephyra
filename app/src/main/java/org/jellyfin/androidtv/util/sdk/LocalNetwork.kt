package org.jellyfin.androidtv.util.sdk

/** Ephyra: an IPv4 network this device is on, as an address and its prefix length (192.168.1.11/24). */
data class LocalNetwork(val address: String, val prefixLength: Int)
