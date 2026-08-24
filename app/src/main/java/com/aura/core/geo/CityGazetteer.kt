package com.aura.core.geo

import javax.inject.Inject
import javax.inject.Singleton

data class City(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

@Singleton
class CityGazetteer @Inject constructor() {

    private val byKey: Map<String, City> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CITIES.associateBy { it.name.normalizeCityKey() }
    }

    val all: List<City> get() = CITIES

    fun find(name: String?): City? {
        val key = name?.normalizeCityKey().orEmpty()
        if (key.isEmpty()) return null
        return byKey[key]
    }

    fun findAll(names: List<String>): List<City> = names.mapNotNull(::find).distinctBy(City::name)

    private companion object {
        val CITIES = listOf(
        City("Anchorage", 61.22, -149.90),
        City("Vancouver", 49.28, -123.12),
        City("Seattle", 47.61, -122.33),
        City("San Francisco", 37.77, -122.42),
        City("Los Angeles", 34.05, -118.24),
        City("Denver", 39.74, -104.99),
        City("Dallas", 32.78, -96.80),
        City("Chicago", 41.88, -87.63),
        City("Toronto", 43.65, -79.38),
        City("Montreal", 45.50, -73.57),
        City("New York", 40.71, -74.01),
        City("Atlanta", 33.75, -84.39),
        City("Miami", 25.76, -80.19),
        City("Mexico City", 19.43, -99.13),
        City("Panama City", 8.98, -79.52),
        City("Bogota", 4.71, -74.07),
        City("Caracas", 10.48, -66.90),
        City("Lima", -12.05, -77.04),
        City("Santiago", -33.45, -70.67),
        City("Buenos Aires", -34.60, -58.38),
        City("Sao Paulo", -23.55, -46.63),
        City("Rio de Janeiro", -22.91, -43.17),
        City("Reykjavik", 64.15, -21.94),
        City("Dublin", 53.35, -6.26),
        City("London", 51.51, -0.13),
        City("Lisbon", 38.72, -9.14),
        City("Madrid", 40.42, -3.70),
        City("Barcelona", 41.39, 2.17),
        City("Paris", 48.86, 2.35),
        City("Amsterdam", 52.37, 4.90),
        City("Zurich", 47.38, 8.54),
        City("Milan", 45.46, 9.19),
        City("Rome", 41.90, 12.50),
        City("Berlin", 52.52, 13.40),
        City("Copenhagen", 55.68, 12.57),
        City("Oslo", 59.91, 10.75),
        City("Stockholm", 59.33, 18.07),
        City("Helsinki", 60.17, 24.94),
        City("Vienna", 48.21, 16.37),
        City("Prague", 50.08, 14.44),
        City("Warsaw", 52.23, 21.01),
        City("Bucharest", 44.43, 26.10),
        City("Athens", 37.98, 23.73),
        City("Istanbul", 41.01, 28.98),
        City("Kyiv", 50.45, 30.52),
        City("Saint Petersburg", 59.93, 30.34),
        City("Moscow", 55.76, 37.62),
        City("Yekaterinburg", 56.84, 60.61),
        City("Novosibirsk", 55.03, 82.92),
        City("Almaty", 43.24, 76.89),
        City("Tashkent", 41.30, 69.24),
        City("Tel Aviv", 32.09, 34.78),
        City("Cairo", 30.04, 31.24),
        City("Casablanca", 33.57, -7.59),
        City("Lagos", 6.52, 3.38),
        City("Accra", 5.60, -0.19),
        City("Kinshasa", -4.44, 15.27),
        City("Addis Ababa", 9.03, 38.74),
        City("Nairobi", -1.29, 36.82),
        City("Johannesburg", -26.20, 28.05),
        City("Cape Town", -33.92, 18.42),
        City("Dubai", 25.20, 55.27),
        City("Doha", 25.29, 51.53),
        City("Riyadh", 24.71, 46.68),
        City("Tehran", 35.69, 51.39),
        City("Karachi", 24.86, 67.01),
        City("Mumbai", 19.08, 72.88),
        City("Delhi", 28.61, 77.21),
        City("Bengaluru", 12.97, 77.59),
        City("Chennai", 13.08, 80.27),
        City("Colombo", 6.93, 79.86),
        City("Dhaka", 23.81, 90.41),
        City("Bangkok", 13.76, 100.50),
        City("Hanoi", 21.03, 105.85),
        City("Ho Chi Minh City", 10.82, 106.63),
        City("Kuala Lumpur", 3.14, 101.69),
        City("Singapore", 1.35, 103.82),
        City("Jakarta", -6.21, 106.85),
        City("Manila", 14.60, 120.98),
        City("Hong Kong", 22.32, 114.17),
        City("Shenzhen", 22.54, 114.06),
        City("Taipei", 25.03, 121.57),
        City("Shanghai", 31.23, 121.47),
        City("Beijing", 39.90, 116.41),
        City("Seoul", 37.57, 126.98),
        City("Tokyo", 35.68, 139.65),
        City("Osaka", 34.69, 135.50),
        City("Perth", -31.95, 115.86),
        City("Brisbane", -27.47, 153.03),
        City("Sydney", -33.87, 151.21),
        City("Melbourne", -37.81, 144.96),
        City("Auckland", -36.85, 174.76),
        City("Honolulu", 21.31, -157.86),
        )
    }
}

private val NON_LETTERS = Regex("""[^\p{L}\p{Nd}]""")

fun String.normalizeCityKey(): String = trim()
    .lowercase()
    .replace(NON_LETTERS, "")
