package co.verifik.wallet.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class TimeUtil {
    companion object {
        @JvmStatic
        fun convertUtcToLocal(
            utcTimeString: String,
            utcFormat: String,
            localFormat: String,
        ): String? {
            try {
                // Define the time zone for UTC
                val utcTimeZone = TimeZone.getTimeZone("UTC")

                // Create a SimpleDateFormat object for UTC format
                val utcFormatter = SimpleDateFormat(utcFormat)
                utcFormatter.timeZone = utcTimeZone

                // Parse the UTC time string into a Date object
                val utcDate: Date = utcFormatter.parse(utcTimeString)!!

                // Define the time zone for local time
                val localTimeZone = TimeZone.getDefault() // Use the device's default time zone

                // Create a SimpleDateFormat object for local format
                val localFormatter = SimpleDateFormat(localFormat)
                localFormatter.timeZone = localTimeZone

                // Format the Date object into a local time string
                return localFormatter.format(utcDate)
            } catch (e: ParseException) {
                e.printStackTrace()
                return null // Handle the parsing exception
            }
        }
    }
}
