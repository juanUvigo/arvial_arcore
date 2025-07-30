package ar.core.ARVIAL.geospatial

import com.google.ar.core.Anchor
import com.google.ar.core.Earth
import android.location.Location

data class Coords(val lat: Double, val lon: Double, val alt: Double = 0.0) {

    // decimal positions: 5 - m, 6 - dcm, 7 - cm,
    fun similar(second:Coords):Boolean = // TODO - Use an actual method to compare distances
        ((lat - second.lat) < 1e-6 && (lon - second.lon) < 1e-6)

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}

class AnchorManager() {

    private var currentLocation: Coords = Coords(0.0,0.0,0.0)

    // ,[^,]+,[^,]+,[^,]+,[^,]+$
    private val locations = mutableSetOf<Coords>(

        // Clavo Geográfico
        Coords(42.170408, -8.690083, 489.257),

        // Prueba
//        Coords(42.1701425,-8.6900235,480.482),

        // CUVI
        Coords(42.1731869083463,-8.6830147626072,481.112+55.89),
        Coords(42.1732383701162,-8.68368018160083,485.027+55.89),
        Coords(42.1732388115965,-8.68369984214693,485.046+55.89),
        Coords(42.173243178603,-8.68370271410511,485.105+55.89),
        Coords(42.1732883822679,-8.68409519926036,486.514+55.89),
        Coords(42.1733972969866,-8.68478957482758,487.502+55.89),
        Coords(42.1735280371342,-8.68488921337041,487.33+55.89),
        Coords(42.1734850789171,-8.68465401758511,487.232+55.89),
        Coords(42.17335759703,-8.68371954266698,484.867+55.89),
        Coords(42.1732986425393,-8.68300209706323,480.54+55.89),
        Coords(42.1732241029113,-8.68249950750089,478.488+55.89),
        Coords(42.1731847696398,-8.68235547859271,478.096+55.89),
        Coords(42.1731835605757,-8.68221158591785,477.713+55.89),
        Coords(42.1732339594044,-8.682201514356,477.443+55.89),
        Coords(42.1732752226418,-8.68233340179841,477.76+55.89),

        // Camelias
        Coords(42.2311217627149,-8.72985150101623,65.346+55.86),
        Coords(42.2311760135509,-8.72980430981664,65.55+55.86),
        Coords(42.2312711755903,-8.72979349399482,65.21+55.86),
        Coords(42.2339823412677,-8.72837573760695,68.062+55.86),
        Coords(42.2342316402304,-8.72814210005064,67.897+55.86),
        Coords(42.2344913646164,-8.72775142235393,67.729+55.86),
        Coords(42.234642158594,-8.72730853965704,68.338+55.86),
        Coords(42.234883989479,-8.72658342605594,67.857+55.86),
        Coords(42.2348211329225,-8.726095759377,68.059+55.86),
        Coords(42.2346965083099,-8.72596562746194,69.353+55.86),
        Coords(42.2346319943028,-8.72556926497173,69.312+55.86),
        Coords(42.2346475540486,-8.72543247931381,68.144+55.86),
        Coords(42.2345741015626,-8.7252305504234,67.549+55.86),
        Coords(42.2348708774798,-8.72550168252043,68.475+55.86),
        Coords(42.2349565649961,-8.7257772577404,68.148+55.86),
        Coords(42.2350064577754,-8.72610433843504,67.598+55.86),
        Coords(42.2350279545294,-8.7268554065454,67.48+55.86),
        Coords(42.2350047646813,-8.72692027264875,67.472+55.86),
        Coords(42.2347635353747,-8.72750519912717,67.611+55.86),
        Coords(42.2346565967747,-8.72771465716651,67.611+55.86),
        Coords(42.2345156247841,-8.72797042313881,67.762+55.86),
        Coords(42.2309653788007,-8.73006631624479,65.391+55.86),
        Coords(42.2308873136656,-8.73009753922418,65.256+55.86),
    )

    // Distance, coords
    var sortedLocations = mutableListOf<Pair<Float, Coords>>()

    // Locations which has been registered in ARCore as anchors
    val setAnchors = HashMap<Coords, Anchor>()
    // val sortedAnchors = mutableListOf<Triple<Float, Coords, Anchor>>()
    val anchorCount: Int
        get() = setAnchors.size

    /**
     *  42.1701106557642, -8.69011710945671, 482.90340614225715, -5.6318515E-7, -0.99999875, -2.6531606E-7, 0.0015692402
     *       42.1700512263000, -8.69009029365590, 483.3349350588396, 6.158973E-8, -0.9933579, 4.4743905E-7, 0.11506535
     *       42.170015513340, -8.69007610270367, 483.68724593892694, 0.04134275, -0.7797473, 0.019376187, -0.6244273
     *       42.1701636590078, -8.6901454493222, 481.3336295336485, 3.775919E-7, 0.1644599, -1.3712355E-7, 0.9863838
     *       42.170217537161, -8.69015511196809, 482.18281569331884, 0.030171772, -0.79337955, -0.1902017, 0.57746154
     *       42.1702757155296, -8.69014501270518, 482.851028168574, 0.0018437093, -0.3218408, 0.0049750567, 0.94677895
     *       42.1703158746902, -8.6901158647734, 483.6744375191629, 0.0022447105, -0.4110377, 0.004814349, 0.91160285
     *       42.1703468631985, -8.69008557822029, 484.3168470710516, 0.0026496195, -0.48769158, 0.004604165, 0.87299985
     *
     *
     *  CON INTERNET!!!!!!!!!!!
     *       42.170195727158045, -8.690163344186347, 481.8582522040233, -0.0016992156, -0.9903904, 0.0041769305, 0.13822633
     *       42.17015175385782, -8.690145197547649, 481.2356068370864, -0.0023454486, -0.9689849, 0.0039259274, 0.2470777
     *       42.17012034075145, -8.690119788548191, 480.97379315830767, -0.001554518, -0.97040594, 0.0028792603, 0.24145734
     *       42.17009501029836, -8.6900889612762, 481.3844555625692, 2.1813037E-6, -0.9635425, 4.068536E-5, 0.2675552
     *
     * Clavo geográfico (minas aparcamiento atrás)
     * 42.170408, -8.690083, 489,257m (433,367m egm96 + 55.89m)
     *     //        val anchor10 = earth.createAnchor(
     *     //            42.170408,
     *     //            -8.690083,
     *     //            489.257,
     *     //            -5.6318515E-7f,
     *     //            -0.99999875f,
     *     //            -2.6531606E-7f,
     *     //            0.0015692402f
     *     //        )
     */


    // quaternion (qx, qy, qz, qw), hay uno que es la identidad
    //      5.6225293E-8f,
    //      -0.36064374f,
    //      -1.4483432E-7f,
    //      0.9327036f
    private val defaultQuaternion = floatArrayOf(5.6225293E-8f, -0.36064374f, -1.4483432E-7f, 0.9327036f)
    private val identityQuaternion = floatArrayOf(0f, 0f, 0f, 1f)

    /**
     * Sets new location
     * update location coordinate list???
     */
    fun updateLocation(earth: Earth) {
        val newLocation = earth.getLocationCoords()
        if(!newLocation.similar(currentLocation)) {
            currentLocation = newLocation
            setMostAdequateAnchors(earth.getLocationCoords(), earth, MAX_ANCHOR_COUNT, maxDistance = 50.0)
        }
    }

    /**
     * Añade una location como anchor a ARCore
     */
    private fun setLocationAsAnchor(location:Coords, earth: Earth) {
        if (location !in locations) locations.add(location) // New location
        if (anchorCount >= MAX_ANCHOR_COUNT) {
            // TODO - Too many anchors
            setMostAdequateAnchors(earth.getLocationCoords(), earth, MAX_ANCHOR_COUNT)
        }
        val anchor = earth.createAnchor(
            location.lat,
            location.lon,
            location.alt,
            defaultQuaternion[0],
            defaultQuaternion[1],
            defaultQuaternion[2],
            defaultQuaternion[3]
        )
        setAnchors[location] = anchor

    }

    fun setNewLocationAsAnchor(location:Coords, earth: Earth, qx:Float, qy:Float, qz:Float, qw:Float) {
        val anchor = earth.createAnchor(location.lat, location.lon, location.alt,
            qx, qy, qz, qw)
        setAnchors[location] = anchor
    }

    /**
     * Elimina anchor de ARCore
     */
    private fun removeLocationAsAnchor(location:Coords) {
        val anchor = setAnchors[location]
        anchor?.detach()
        setAnchors.remove(location)
    }

    fun removeAllAnchors() {
        setAnchors.forEach{ (location, anchor) ->
            anchor.detach()
        }
        setAnchors.clear()
    }


    private fun sortLocations(currentCoord: Coords): List<Pair<Float, Coords>> {

        // Get all the distances between current position and every anchor.
        // Distance, Coords (lat, lon, alt), ARCore Anchor
        val distanceList = mutableListOf<Pair<Float, Coords>>()
        locations.forEach{coords ->
            distanceList.add(
                Pair(
                    distanceBetweenCoords(currentCoord, coords),
                    coords
                )
            )
        }

        // Sort from closest to furthest away
        val closestToFurthestAwayLocation = distanceList.sortedWith { a1, a2 ->
            a1.first.compareTo(a2.first)
        }

        sortedLocations = closestToFurthestAwayLocation.toMutableList()

        return closestToFurthestAwayLocation
    }

    /**
     * Returns distance between 2 WSG84 coords in meters
     * lat1, lon1 : Coords1
     * lat2, lon2 : Coords2
     */
    private fun distanceBetweenCoords(coords1: Coords, coords2: Coords): Float {
        val startPoint = Location("startPoint").apply {
            latitude = coords1.lat
            longitude = coords1.lon
            altitude = coords1.alt
        }

        val endPoint = Location("endPoint").apply {
            latitude = coords2.lat
            longitude = coords2.lon
            altitude = coords2.alt
        }

        return startPoint.distanceTo(endPoint) // Distance in meters
    }

    /**
     * Order full list of locations and set as many as possible by proximity
     * max: ammout | distance
     */
    private fun setMostAdequateAnchors(
        currentCoords: Coords,
        earth: Earth,
        maxAmmount: Int,
        maxDistance: Double = Double.MAX_VALUE
    ) {

        // Generate the list of sorted anchors
        val sortedLocations = sortLocations(currentCoords)

        // Find which anchors are within the max distance set
        val locationsToBeAdded: MutableSet<Coords> = mutableSetOf()
        val trimmedSortedLocations = sortedLocations.slice(0..maxAmmount)
        for((distance, location) in trimmedSortedLocations) {
            if (distance > maxDistance) break  // The list is too big already

            locationsToBeAdded.add(location)
        }

        // Remove far away anchors
        val locationsToBeRemoved = mutableSetOf<Coords>()
        setAnchors.forEach { (coords, anchor) ->
            if (coords !in locationsToBeAdded) locationsToBeRemoved.add(coords)
        }
        locationsToBeRemoved.forEach { coords -> removeLocationAsAnchor(coords) }

        // Add missing close anchors
        locationsToBeAdded.forEach { coords ->
            if (coords !in setAnchors) setLocationAsAnchor(coords, earth)
        }

    }

    fun anchorAllLocations(earth: Earth) {
        updateLocation(earth)
//        setMostAdequateAnchors(earth.getLocationCoords(), earth, maxAmmount = MAX_ANCHOR_COUNT)
    }

    companion object {
        const val MAX_ANCHOR_COUNT = 20
    }
}

fun Earth.getLocationCoords(): Coords =
    Coords(this.cameraGeospatialPose.latitude, this.cameraGeospatialPose.longitude, this.cameraGeospatialPose.altitude)




