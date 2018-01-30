package com.yaleiden.gps.gpsaccuracy

/**
 * Created by Yale on 1/29/2018.
 */
data class NmeaParser(val string: String) {

    /*
    $GPRMC,220516,A,5133.82,N,00042.24,W,173.8,231.8,130694,004.2,W*70
              1    2    3    4    5     6    7    8      9     10  11 12


      1   220516     Time Stamp
      2   A          validity - A-ok, V-invalid
      3   5133.82    current Latitude
      4   N          North/South
      5   00042.24   current Longitude
      6   W          East/West
      7   173.8      Speed in knots
      8   231.8      True course
      9   130694     Date Stamp
      10  004.2      Variation
      11  W          East/West
      12  *70        checksum
     */

    fun parseRmc(): List<String> {

        val result: List<String>  = string.split(",".toRegex())

        return result
    }


}