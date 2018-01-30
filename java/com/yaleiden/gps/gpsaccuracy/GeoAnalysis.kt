package com.yaleiden.gps.gpsaccuracy

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil.computeDistanceBetween

/**
 * Created by Yale on 1/22/2018.
 *
 * Calling the getBounds() method
 * Returns an array of Double values that represent
 * the southwest and northeast Lat and Lng values,
 * Average Lat and Lng values, and average error.
 * @param array of Location objects
 */
//data class GeoAnalysis(val locations: Array<Location>, var minLat: Double, var maxLat: Double, var minLng: Double, var maxLng: Double) {
data class GeoAnalysis(val locations: Array<Location>) {
    lateinit var numArray: DoubleArray

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }


    /**
     * Returns an array of Double values that represent
     * the southwest and northeast Lat and Lng values,
     * Average Lat and Lng values, and average error.
     * @param and array of Location objects
     */

    fun getBounds(): Array<Double> {
        //End of range starting values
        val minLat = 90.0
        val maxLat = -90.0
        val minLng = 180.0
        val maxLng = -180.0
        var avgLat = 0.0
        var avgLng = 0.0
        var avgError = 0.0
        var stdDev = 0.0
        var count = 0

        var result = arrayOf(minLat, minLng, maxLat, maxLng, avgLat, avgLng, avgError, stdDev)
        //iterate through array to find min and max
        for (location in locations) {
            avgLat += location.latitude
            avgLng += location.longitude
            count += 1

            if (location.latitude < result.get(0)) {
                result.set(0, location.latitude)
            }
            if (location.longitude < result.get(1)) {
                result.set(1, location.longitude)
            }
            if (location.latitude > result.get(2)) {
                result.set(2, location.latitude)
            }
            if (location.longitude > result.get(3)) {
                result.set(3, location.longitude)
            }
        }

        avgLat /= count
        avgLng /= count

        result.set(4, avgLat)
        result.set(5, avgLng)

        val avgErr: Double = getAvgError(locations, LatLng(avgLat, avgLng))
        result.set(6, avgErr)
        result.set(7, calculateSD(avgErr, count))

        return (result)
    }

    fun getAvgError(locations: Array<Location>, avg: LatLng): Double {

        var errorSum: Double = 0.0
        var count = 0
        numArray = DoubleArray(size = locations.size)
        for (location in locations) {

            val dist: Double = computeDistanceBetween(avg,
                    LatLng(locations[count].latitude, locations[count].longitude))
            errorSum += dist

            numArray.set(count, dist)

            count += 1
        }
        return errorSum / count

    }


    fun calculateSD(mean: Double, count: Int): Double {

        var sumDif = 0.0

        for (num in numArray) {
            sumDif += Math.pow(num - mean, 2.0)
        }
        val variance = sumDif / count
        return Math.sqrt(variance)
    }
}