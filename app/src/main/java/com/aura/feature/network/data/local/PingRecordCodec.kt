package com.aura.feature.network.data.local

import com.aura.feature.network.domain.model.PING_HISTORY_LIMIT
import com.aura.feature.network.domain.model.PingRecord

private const val RECORD_SEPARATOR = '\u001E'

private const val FIELD_SEPARATOR = '\u001F'

private const val FIELD_COUNT = 6

private const val VPN_ON = "1"

fun List<PingRecord>.appendCapped(record: PingRecord): List<PingRecord> =
    (this + record).takeLast(PING_HISTORY_LIMIT)

fun List<PingRecord>.encodeRecords(): String = joinToString(RECORD_SEPARATOR.toString()) { record ->
    listOf(
        record.timestamp.toString(),
        record.ipAddress.orEmpty(),
        record.operator.orEmpty(),
        record.pingMs.toString(),
        record.location.orEmpty(),
        if (record.vpnActive) VPN_ON else "",
    ).joinToString(FIELD_SEPARATOR.toString())
}

fun String?.decodeRecords(): List<PingRecord> {
    if (isNullOrEmpty()) return emptyList()

    return split(RECORD_SEPARATOR).mapNotNull { line ->
        val fields = line.split(FIELD_SEPARATOR)
        if (fields.size != FIELD_COUNT) return@mapNotNull null

        val timestamp = fields[0].toLongOrNull() ?: return@mapNotNull null
        val pingMs = fields[3].toIntOrNull() ?: return@mapNotNull null

        PingRecord(
            timestamp = timestamp,
            ipAddress = fields[1].ifEmpty { null },
            operator = fields[2].ifEmpty { null },
            pingMs = pingMs,
            location = fields[4].ifEmpty { null },
            vpnActive = fields[5] == VPN_ON,
        )
    }
}
