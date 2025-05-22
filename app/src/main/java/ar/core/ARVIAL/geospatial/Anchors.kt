package ar.core.ARVIAL.geospatial

import com.google.ar.core.Anchor
import com.google.ar.core.Earth
import android.location.Location

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
 */
fun anchorList(earth: Earth):List<Anchor> {

    val anchors = mutableListOf<Anchor>()

    val anchor1 = earth.createAnchor(
        42.17013358337541,
        -8.690065687532542,
        486.0568971633911,
        5.6225293E-8f,
        -0.36064374f,
        -1.4483432E-7f,
        0.9327036f
    )
    // Asfalto y pasarela en el suelo
    val anchor2 = earth.createAnchor(
        42.1701106557642,
        -8.69011710945671,
        482.90340614225715,
        -5.6318515E-7f,
        -0.99999875f,
        -2.6531606E-7f,
        0.0015692402f
    )
    val anchor3 = earth.createAnchor(
        42.1700512263000,
        -8.69009029365590,
        483.3349350588396,
        6.158973E-8f,
        -0.9933579f,
        4.4743905E-7f,
        0.11506535f
    )
    val anchor4 = earth.createAnchor(
        42.170015513340,
        -8.69007610270367,
        483.68724593892694,
        0.04134275f,
        -0.7797473f,
        0.019376187f,
        -0.6244273f
    )
    val anchor5 = earth.createAnchor(
        42.1701636590078,
        -8.6901454493222,
        481.3336295336485,
        3.775919E-7f,
        0.1644599f,
        -1.3712355E-7f,
        0.9863838f
    )
    val anchor6 = earth.createAnchor(
        42.170217537161,
        -8.69015511196809,
        482.18281569331884,
        0.030171772f,
        -0.79337955f,
        -0.1902017f,
        0.57746154f
    )
    val anchor7 = earth.createAnchor(
        42.1702757155296,
        -8.69014501270518,
        482.851028168574,
        0.0018437093f,
        -0.3218408f,
        0.0049750567f,
        0.94677895f
    )
    val anchor8 = earth.createAnchor(
        42.1703158746902,
        -8.6901158647734,
        483.6744375191629,
        0.0022447105f,
        -0.4110377f,
        0.004814349f,
        0.91160285f
    )
    val anchor9 = earth.createAnchor(
        42.1703468631985,
        -8.69008557822029,
        484.3168470710516,
        0.0026496195f,
        -0.48769158f,
        0.004604165f,
        0.87299985f
    )

    // Clavo geográfico (minas aparcamiento atrás)
    // 42.170408, -8.690083, 489,257m (433,367m egm96 + 55.89m)
    val anchor10 = earth.createAnchor(
        42.170408,
        -8.690083,
        489.257,
        -5.6318515E-7f,
        -0.99999875f,
        -2.6531606E-7f,
        0.0015692402f
    )

//      anchors.add(anchor1);
//      anchors.add(anchor2);
//      anchors.add(anchor3);
//      anchors.add(anchor4);
//      anchors.add(anchor5);
//      anchors.add(anchor6);
//      anchors.add(anchor7);
//      anchors.add(anchor8);
//      anchors.add(anchor9);
    anchors.add(anchor10)

    return anchors
}

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val startPoint = Location("startPoint").apply {
        latitude = lat1
        longitude = lon1
    }

    val endPoint = Location("endPoint").apply {
        latitude = lat2
        longitude = lon2
    }

    return startPoint.distanceTo(endPoint) // Distance in meters
}

fun onlyCloseAnchors(threshold: Float, currenLoc: Pair<Double, Double>, anchors: List<Anchor>) {
    for (anchor in anchors) {
        anchor.ge
    }
}

