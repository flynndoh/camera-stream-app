package com.example.cam_streamer.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.util.Log
import java.net.Inet4Address
import java.net.InetAddress

object NetworkHelper {

    fun getLocalIpAddress(context: Context): String? {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return null
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null

            // Check for Wi-Fi or Ethernet connection
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                val linkProperties: LinkProperties = connectivityManager.getLinkProperties(activeNetwork) ?: return null
                for (linkAddress: LinkAddress in linkProperties.linkAddresses) {
                    val address: InetAddress = linkAddress.address
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkHelper", "Error retrieving local IP address", e)
        }
        return null
    }
}
