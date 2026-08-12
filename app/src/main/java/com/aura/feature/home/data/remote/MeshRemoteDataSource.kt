package com.aura.feature.home.data.remote

import com.aura.core.geo.IpInfoSource
import com.aura.core.network.NetworkMonitor
import com.aura.feature.home.data.remote.dto.MeshCityDto
import com.aura.feature.home.data.remote.dto.MeshSnapshotDto
import com.aura.feature.home.data.remote.dto.UserLocationDto
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

interface MeshRemoteDataSource {
    suspend fun fetchMeshSnapshot(): MeshSnapshotDto

    suspend fun fetchUserLocation(): UserLocationDto
}

@Singleton
class MockMeshRemoteDataSource @Inject constructor(
    private val ipInfoSource: IpInfoSource,
    private val networkMonitor: NetworkMonitor,
) : MeshRemoteDataSource {

    override suspend fun fetchMeshSnapshot(): MeshSnapshotDto {
        delay(NETWORK_DELAY_MILLIS)
        return MeshSnapshotDto(
            cities = CITIES.map { it.copy(live = it.id in LIVE_CITY_IDS) },
            nodesOnline = NODES_ONLINE,
        )
    }

    override suspend fun fetchUserLocation(): UserLocationDto {
        val info = ipInfoSource.fetch()
        return UserLocationDto(
            lat = info.latitude,
            lon = info.longitude,
            city = info.city,
            vpnActive = networkMonitor.current().isVpnActive,
        )
    }

    private companion object {
        const val NETWORK_DELAY_MILLIS = 350L
        const val NODES_ONLINE = 4210

        val LIVE_CITY_IDS = setOf("toronto", "moscow", "sydney")

        val CITIES = listOf(
            city("anchorage", "Anchorage", 61.22, -149.90),
            city("vancouver", "Vancouver", 49.28, -123.12),
            city("seattle", "Seattle", 47.61, -122.33),
            city("san-francisco", "San Francisco", 37.77, -122.42),
            city("los-angeles", "Los Angeles", 34.05, -118.24),
            city("denver", "Denver", 39.74, -104.99),
            city("dallas", "Dallas", 32.78, -96.80),
            city("chicago", "Chicago", 41.88, -87.63),
            city("toronto", "Toronto", 43.65, -79.38),
            city("montreal", "Montreal", 45.50, -73.57),
            city("new-york", "New York", 40.71, -74.01),
            city("atlanta", "Atlanta", 33.75, -84.39),
            city("miami", "Miami", 25.76, -80.19),
            city("mexico-city", "Mexico City", 19.43, -99.13),
            city("panama", "Panama City", 8.98, -79.52),
            city("bogota", "Bogota", 4.71, -74.07),
            city("caracas", "Caracas", 10.48, -66.90),
            city("lima", "Lima", -12.05, -77.04),
            city("santiago", "Santiago", -33.45, -70.67),
            city("buenos-aires", "Buenos Aires", -34.60, -58.38),
            city("sao-paulo", "Sao Paulo", -23.55, -46.63),
            city("rio", "Rio de Janeiro", -22.91, -43.17),
            city("reykjavik", "Reykjavik", 64.15, -21.94),
            city("dublin", "Dublin", 53.35, -6.26),
            city("london", "London", 51.51, -0.13),
            city("lisbon", "Lisbon", 38.72, -9.14),
            city("madrid", "Madrid", 40.42, -3.70),
            city("barcelona", "Barcelona", 41.39, 2.17),
            city("paris", "Paris", 48.86, 2.35),
            city("amsterdam", "Amsterdam", 52.37, 4.90),
            city("zurich", "Zurich", 47.38, 8.54),
            city("milan", "Milan", 45.46, 9.19),
            city("rome", "Rome", 41.90, 12.50),
            city("berlin", "Berlin", 52.52, 13.40),
            city("copenhagen", "Copenhagen", 55.68, 12.57),
            city("oslo", "Oslo", 59.91, 10.75),
            city("stockholm", "Stockholm", 59.33, 18.07),
            city("helsinki", "Helsinki", 60.17, 24.94),
            city("vienna", "Vienna", 48.21, 16.37),
            city("prague", "Prague", 50.08, 14.44),
            city("warsaw", "Warsaw", 52.23, 21.01),
            city("bucharest", "Bucharest", 44.43, 26.10),
            city("athens", "Athens", 37.98, 23.73),
            city("istanbul", "Istanbul", 41.01, 28.98),
            city("kyiv", "Kyiv", 50.45, 30.52),
            city("saint-petersburg", "Saint Petersburg", 59.93, 30.34),
            city("moscow", "Moscow", 55.76, 37.62),
            city("yekaterinburg", "Yekaterinburg", 56.84, 60.61),
            city("novosibirsk", "Novosibirsk", 55.03, 82.92),
            city("almaty", "Almaty", 43.24, 76.89),
            city("tashkent", "Tashkent", 41.30, 69.24),
            city("tel-aviv", "Tel Aviv", 32.09, 34.78),
            city("cairo", "Cairo", 30.04, 31.24),
            city("casablanca", "Casablanca", 33.57, -7.59),
            city("lagos", "Lagos", 6.52, 3.38),
            city("accra", "Accra", 5.60, -0.19),
            city("kinshasa", "Kinshasa", -4.44, 15.27),
            city("addis-ababa", "Addis Ababa", 9.03, 38.74),
            city("nairobi", "Nairobi", -1.29, 36.82),
            city("johannesburg", "Johannesburg", -26.20, 28.05),
            city("cape-town", "Cape Town", -33.92, 18.42),
            city("dubai", "Dubai", 25.20, 55.27),
            city("doha", "Doha", 25.29, 51.53),
            city("riyadh", "Riyadh", 24.71, 46.68),
            city("tehran", "Tehran", 35.69, 51.39),
            city("karachi", "Karachi", 24.86, 67.01),
            city("mumbai", "Mumbai", 19.08, 72.88),
            city("delhi", "Delhi", 28.61, 77.21),
            city("bengaluru", "Bengaluru", 12.97, 77.59),
            city("chennai", "Chennai", 13.08, 80.27),
            city("colombo", "Colombo", 6.93, 79.86),
            city("dhaka", "Dhaka", 23.81, 90.41),
            city("bangkok", "Bangkok", 13.76, 100.50),
            city("hanoi", "Hanoi", 21.03, 105.85),
            city("ho-chi-minh", "Ho Chi Minh City", 10.82, 106.63),
            city("kuala-lumpur", "Kuala Lumpur", 3.14, 101.69),
            city("singapore", "Singapore", 1.35, 103.82),
            city("jakarta", "Jakarta", -6.21, 106.85),
            city("manila", "Manila", 14.60, 120.98),
            city("hong-kong", "Hong Kong", 22.32, 114.17),
            city("shenzhen", "Shenzhen", 22.54, 114.06),
            city("taipei", "Taipei", 25.03, 121.57),
            city("shanghai", "Shanghai", 31.23, 121.47),
            city("beijing", "Beijing", 39.90, 116.41),
            city("seoul", "Seoul", 37.57, 126.98),
            city("tokyo", "Tokyo", 35.68, 139.65),
            city("osaka", "Osaka", 34.69, 135.50),
            city("perth", "Perth", -31.95, 115.86),
            city("brisbane", "Brisbane", -27.47, 153.03),
            city("sydney", "Sydney", -33.87, 151.21),
            city("melbourne", "Melbourne", -37.81, 144.96),
            city("auckland", "Auckland", -36.85, 174.76),
            city("honolulu", "Honolulu", 21.31, -157.86),
        )

        fun city(id: String, name: String, lat: Double, lon: Double) =
            MeshCityDto(id = id, name = name, lat = lat, lon = lon, live = false)
    }
}
