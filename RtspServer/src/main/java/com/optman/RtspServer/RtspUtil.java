/*
 * Copyright (c) Novetta Solutions, LLC
 * All rights reserved. No warranty, explicit or implicit, provided.
 */

package com.optman.RtspServer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.Network;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;

public class RtspUtil {
    private static final String VIXED_ID_PREF = "VIXED_ID_PREF";

    public static final int PORT = 8554;

    // I don't know how the RTSP server determines this now how we get this from the class directly.
    // URL will be like: ...:6666/ch0
    public static final String CAMERA_ID = "ch0";

    /**
     * Returns the last inet addr in the list. This assumes the VPN will apways be the last. (Probably a safe bet for PoC.)
     *
     * @return Either an address or an empty string.
     */
    public static String getLastInetAddress(final Context context) {
        String                    lastAddr            = "";
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Loop all networks.
        for (Network network : connectivityManager.getAllNetworks()) {
            // If connected.
            if (connectivityManager.getNetworkInfo(network).isConnected()) {
                // Loop addresses
                for (LinkAddress address : connectivityManager.getLinkProperties(network).getLinkAddresses()) {
                    lastAddr = address.getAddress().getHostAddress();
                }
            }
        }

        return lastAddr;
    }

    @NonNull
    public static String getRtspUrl(final Context context) {
        return "rtsp://" + getLastInetAddress(context) + ":" + PORT + "/";
    }

    @NonNull
    public static String getCameraUrl(final Context context) {
        return getRtspUrl(context) + CAMERA_ID;
    }

    @Nullable
    public static String getVidexId(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(VIXED_ID_PREF, null);
    }

    public static void setVidexId(final Context context, @Nullable final String videxId) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(VIXED_ID_PREF, videxId).apply();
    }
}