package com.aura.core.geo

import android.content.Context
import com.aura.core.common.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.text.Normalizer
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val ASSET_NAME = "cities.gz"

private const val SECTION_SEPARATOR = '\u001E'

private const val FIELD_SEPARATOR = ';'

private const val BUFFER_SIZE = 64 * 1024

private const val CACHE_LIMIT = 512

data class City(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

@Singleton
class CityGazetteer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    private val cache = HashMap<String, City?>()
    private val mutex = Mutex()

    suspend fun find(name: String?): City? = findAll(listOfNotNull(name)).firstOrNull()

    suspend fun findAll(names: List<String>): List<City> {
        val requested = names.associateBy { it.normalizeCityKey() }.filterKeys(String::isNotEmpty)
        if (requested.isEmpty()) return emptyList()

        val resolved = LinkedHashMap<String, City>()
        val missing = HashMap<String, String>()

        mutex.withLock {
            requested.forEach { (key, original) ->
                if (cache.containsKey(key)) {
                    cache[key]?.let { resolved[key] = it.copy(name = original) }
                } else {
                    missing[key] = original
                }
            }
        }

        if (missing.isNotEmpty()) {
            val found = withContext(ioDispatcher) { lookup(missing.keys) }

            mutex.withLock {
                if (cache.size > CACHE_LIMIT) cache.clear()
                missing.forEach { (key, original) ->
                    val city = found[key]
                    cache[key] = city
                    if (city != null) resolved[key] = city.copy(name = original)
                }
            }
        }

        return names.mapNotNull { resolved[it.normalizeCityKey()] }
    }

    private fun lookup(keys: Set<String>): Map<String, City> = try {
        context.assets.open(ASSET_NAME).use { raw ->
            GZIPInputStream(raw, BUFFER_SIZE).bufferedReader().use { reader ->
                reader.scan(keys)
            }
        }
    } catch (error: IOException) {
        emptyMap()
    }

    private fun BufferedReader.scan(keys: Set<String>): Map<String, City> {
        val latitudes = ArrayList<Double>(EXPECTED_CITIES)
        val longitudes = ArrayList<Double>(EXPECTED_CITIES)

        while (true) {
            val line = readLine() ?: return emptyMap()
            if (line.length == 1 && line[0] == SECTION_SEPARATOR) break

            val separator = line.indexOf(FIELD_SEPARATOR)
            if (separator <= 0) continue
            val latitude = line.substring(0, separator).toDoubleOrNull() ?: continue
            val longitude = line.substring(separator + 1).toDoubleOrNull() ?: continue
            latitudes += latitude
            longitudes += longitude
        }

        val found = HashMap<String, City>(keys.size)

        while (found.size < keys.size) {
            val line = readLine() ?: break
            val separator = line.indexOf(FIELD_SEPARATOR)
            if (separator <= 0) continue

            val key = line.substring(0, separator)
            if (key !in keys) continue

            val index = line.substring(separator + 1).toIntOrNull() ?: continue
            if (index !in latitudes.indices) continue

            found[key] = City(
                name = key,
                latitude = latitudes[index],
                longitude = longitudes[index],
            )
        }

        return found
    }

    private companion object {
        const val EXPECTED_CITIES = 70_000
    }
}

private val NON_LETTERS = Regex("""[^\p{L}\p{Nd}]""")

private val COMBINING_MARKS = Regex("""\p{Mn}+""")

fun String.normalizeCityKey(): String = Normalizer.normalize(trim(), Normalizer.Form.NFKD)
    .replace(COMBINING_MARKS, "")
    .lowercase()
    .replace(NON_LETTERS, "")
