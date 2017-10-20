package com.optman.RtspServer;

import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Utility {

    static public String getLocalIpAddress() {

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                        String result = inetAddress.getHostAddress().toString();

                        //ignore hotspot address.
                        if (result.equalsIgnoreCase("192.168.43.1")) {
                            Log.i("Utility", "ignore hotspot address");
                            continue;
                        }

                        return result;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("Utility", ex.toString());
        }
        return "";
    }
}
