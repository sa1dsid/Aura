package com.aura.feature.network

internal object Net {

    const val IPV4 = "192.168.1.42"
    const val IPV6 = "2a02:6b8:c02:901:0:fc00:1:2"
    const val OPERATOR = "T-Mobile"
    const val OTHER_OPERATOR = "Vodafone"
    const val LOCATION = "Miami, US"
    const val OTHER_LOCATION = "London, UK"
    const val MEASURED_AT = "2026-08-26T08:14:00.000Z"
    const val PROTOCOL_IPV4 = "IPv4"
    const val PROTOCOL_IPV6 = "IPv6"
    const val WIRE_WIFI = "WIFI"

    fun state(
        ip: String? = IPV4,
        operator: String? = OPERATOR,
        location: String? = LOCATION,
        connection: String? = WIRE_WIFI,
        protocol: String? = null,
        vpn: Boolean = false,
    ): String = obj(
        "ip" to text(ip),
        "operator" to text(operator),
        "location" to text(location),
        "connection" to text(connection),
        "protocol" to text(protocol),
        "vpn" to vpn.toString(),
    )

    fun summary(
        ip: String? = IPV4,
        operator: String? = OPERATOR,
        location: String? = LOCATION,
        connection: String? = WIRE_WIFI,
        protocol: String? = null,
        vpn: Boolean = false,
        lastTestedAt: String? = MEASURED_AT,
        pingMs: String? = "27.000000",
        jitterMs: String? = null,
        packetLossPct: String? = null,
    ): String = obj(
        "ip" to text(ip),
        "operator" to text(operator),
        "location" to text(location),
        "connection" to text(connection),
        "protocol" to text(protocol),
        "vpn" to vpn.toString(),
        "last_tested_at" to text(lastTestedAt),
        "ping_ms" to text(pingMs),
        "jitter_ms" to text(jitterMs),
        "packet_loss_pct" to text(packetLossPct),
    )

    fun ping(
        id: Int = 1,
        ip: String? = IPV4,
        operator: String? = OPERATOR,
        location: String? = LOCATION,
        connection: String? = WIRE_WIFI,
        protocol: String? = PROTOCOL_IPV4,
        vpn: Boolean = false,
        pingMs: String = "27.000000",
        jitterMs: String? = null,
        packetLossPct: String? = null,
        downloadMbps: String? = null,
        uploadMbps: String? = null,
        measuredAt: String = MEASURED_AT,
    ): String = obj(
        "id" to id.toString(),
        "ip" to text(ip),
        "operator" to text(operator),
        "location" to text(location),
        "connection" to text(connection),
        "protocol" to text(protocol),
        "vpn" to vpn.toString(),
        "ping_ms" to text(pingMs),
        "jitter_ms" to text(jitterMs),
        "packet_loss_pct" to text(packetLossPct),
        "download_mbps" to text(downloadMbps),
        "upload_mbps" to text(uploadMbps),
        "measured_at" to text(measuredAt),
    )

    fun list(rows: List<String>): String = rows.joinToString(prefix = "[", postfix = "]")

    fun journal(count: Int, firstPingMs: Int = 20): String = list(
        List(count) { index ->
            ping(
                id = index + 1,
                pingMs = (firstPingMs + index).toString(),
                measuredAt = isoOf(index),
            )
        }
    )

    fun isoAt(day: Int, hour: Int = 8, minute: Int = 0): String =
        "2026-08-%02dT%02d:%02d:00.000Z".format(day, hour, minute)

    fun isoOf(index: Int): String = isoAt(day = 1 + index / MINUTES, minute = index % MINUTES)

    private fun obj(vararg fields: Pair<String, String>): String =
        fields.joinToString(prefix = "{", postfix = "}") { (key, value) -> "\"$key\":$value" }

    private fun text(value: String?): String = if (value == null) "null" else "\"$value\""

    private const val MINUTES = 60
}
