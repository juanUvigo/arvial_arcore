package ar.core.ARVIAL.data

import android.content.SharedPreferences
import android.util.Log
import ar.core.ARVIAL.geospatial.GeospatialActivity.AnchorType
import ar.core.ARVIAL.geospatial.GeospatialActivity.Companion.SHARED_PREFERENCES_SAVED_ANCHORS
import com.google.ar.core.Earth

class LocationsProvider(var sharedPreferences: SharedPreferences) {
    /**
     * Helper function to store the parameters used in anchor creation in [SharedPreferences].
     */
    private fun storeAnchorParameters(
        latitude: Double, longitude: Double, altitude: Double, quaternion: FloatArray
    ) {
        val anchorParameterSet =
            sharedPreferences!!.getStringSet(SHARED_PREFERENCES_SAVED_ANCHORS, HashSet())!!
        val newAnchorParameterSet = HashSet(anchorParameterSet)

        val editor = sharedPreferences!!.edit()
        var type = ""
//        type = when (anchorType) {
//            AnchorType.TERRAIN -> "Terrain"
//            AnchorType.ROOFTOP -> "Rooftop"
//            else -> ""
//        }
        newAnchorParameterSet.add(
            String.format(
                "$type%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f",
                latitude,
                longitude,
                altitude,
                quaternion[0],
                quaternion[1],
                quaternion[2],
                quaternion[3]
            )
        )
        editor.putStringSet(SHARED_PREFERENCES_SAVED_ANCHORS, newAnchorParameterSet)
        editor.commit()
    }


    /** Creates all anchors that were stored in the [SharedPreferences].  */
    private fun createAnchorFromSharedPreferences(earth: Earth) {
        val anchorParameterSet =
            sharedPreferences!!.getStringSet(
                SHARED_PREFERENCES_SAVED_ANCHORS,
                null
            )
                ?: return

        for (anchorParameters in anchorParameterSet) {
            var anchorParameters = anchorParameters
            var type = AnchorType.GEOSPATIAL
            if (anchorParameters.contains("Terrain")) {
                type = AnchorType.TERRAIN
                anchorParameters = anchorParameters.replace("Terrain", "")
            } else if (anchorParameters.contains("Rooftop")) {
                type = AnchorType.ROOFTOP
                anchorParameters = anchorParameters.replace("Rooftop", "")
            }
            val parameters: Array<String> =
                anchorParameters.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parameters.size != 7) {
                Log.d(
                    "LocationsProvider",
                    "Invalid number of anchor parameters. Expected four, found " + parameters.size
                )
                continue
            }
            val latitude: Double = parameters[0].toDouble()
            val longitude: Double = parameters[1].toDouble()
            val altitude: Double = parameters[2].toDouble()
            val quaternion =
                floatArrayOf(
                    parameters[3].toFloat(),
                    parameters[4].toFloat(),
                    parameters[5].toFloat(),
                    parameters[6].toFloat()
                )
//            when (type) {
//                AnchorType.TERRAIN -> createTerrainAnchor(earth, latitude, longitude, quaternion)
//                AnchorType.ROOFTOP -> createRooftopAnchor(earth, latitude, longitude, quaternion)
//                else -> createAnchor(earth, latitude, longitude, altitude, quaternion)
//            }
        }
    }

    private fun clearAnchorsFromSharedPreferences() {
        val editor = sharedPreferences!!.edit()
        editor.putStringSet(SHARED_PREFERENCES_SAVED_ANCHORS, null)
        editor.commit()
    }
}