package org.jellyfin.androidtv.util.sdk

import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URI
import java.net.URISyntaxException

private const val IPV4_BITS = 32
private const val OCTET_BITS = 8
private const val OCTET_MAX = 255
private val ipv4Pattern = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")

private fun ipv4ToInt(address: String): Int? {
	val parts = ipv4Pattern.matchEntire(address)?.groupValues?.drop(1)?.map(String::toInt) ?: return null
	if (parts.any { it > OCTET_MAX }) return null
	return parts.fold(0) { acc, part -> (acc shl OCTET_BITS) or part }
}

private fun LocalNetwork.contains(address: Int): Boolean {
	val network = ipv4ToInt(this.address) ?: return false
	if (prefixLength !in 1..IPV4_BITS) return false
	val mask = -1 shl (IPV4_BITS - prefixLength)
	return network and mask == address and mask
}

private fun parseUri(address: String): URI? = try {
	URI(address)
} catch (_: URISyntaxException) {
	null
}

/**
 * Velella: the address to use for a server found by local discovery. A server in a Docker bridge network announces its container
 * address (http://172.23.0.2:8096), which other devices cannot reach. When the announced host is an IPv4 address outside every
 * network of this device, and the address the reply came from ([endpointAddress]) is inside one, the reply's address is used with
 * the same scheme and port. Host names (and so HTTPS certificates) and reachable addresses are kept as announced.
 */
fun discoveredServerAddress(address: String, endpointAddress: String?, localNetworks: List<LocalNetwork>): String {
	val endpoint = endpointAddress?.let(::ipv4ToInt) ?: return address
	val uri = parseUri(address) ?: return address
	val host = uri.host?.let(::ipv4ToInt) ?: return address

	val unreachable = host != endpoint &&
		localNetworks.none { it.contains(host) } &&
		localNetworks.any { it.contains(endpoint) }
	if (!unreachable) return address

	return URI(uri.scheme, uri.userInfo, endpointAddress, uri.port, uri.path, uri.query, uri.fragment).toString()
}

/** Velella: the IPv4 networks of this device's active interfaces, loopback excluded. Blocking: call it off the main thread. */
fun localNetworks(): List<LocalNetwork> = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
	.filter { it.isUp && !it.isLoopback }
	.flatMap { it.interfaceAddresses }
	.filter { it.address is Inet4Address }
	.mapNotNull { address -> address.address.hostAddress?.let { LocalNetwork(it, address.networkPrefixLength.toInt()) } }
