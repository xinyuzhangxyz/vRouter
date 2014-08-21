package com.example.vrouter.util;

import java.nio.ByteBuffer;

import android.util.Log;

public class DebugTools {

	private final static String TAG = "VRouterService_Debug";
	
	public static void printIPPacket(ByteBuffer packet){
    	// Reference: http://en.wikipedia.org/wiki/IPv4
		//Log.d(TAG, "Original bytes: " + bytesToHex(packet.array()));
        int buffer = packet.get();
        int version;
        int headerlength;
        version = buffer >> 4;
        headerlength = buffer & 0x0F;
        
        headerlength *= 4;
        //Log.d(TAG, "IP Version:"+version);
        //Log.d(TAG, "Header Length:"+headerlength);

        String status = "";
        //status += "Header Length:"+headerlength;

        buffer = packet.get();      //DSCP + EN
        buffer = packet.getChar();  //Total Length
        
        //Log.d(TAG, "Total Length:"+buffer);

        buffer = packet.getChar();  //Identification
        buffer = packet.getChar();  //Flags + Fragment Offset
        buffer = packet.get();      //Time to Live
        buffer = packet.get();      //Protocol
        
        //Log.d(TAG, "Protocol:"+buffer);

        status += "  Protocol:"+buffer;
        
        buffer = packet.getChar();  //Header checksum
        
        String sourceIP  = "";
        buffer = packet.get();  //Source IP 1st Octet FIXME
        sourceIP += buffer;
        sourceIP += ".";

        buffer = packet.get();  //Source IP 2nd Octet
        sourceIP += buffer;
        sourceIP += ".";

        buffer = packet.get();  //Source IP 3rd Octet
        sourceIP += buffer;
        sourceIP += ".";

        buffer = packet.get();  //Source IP 4th Octet
        sourceIP += buffer;

        Log.d(TAG, "Source IP:"+sourceIP);

        //status += "   Source IP:"+sourceIP;

        String destIP  = "";
        buffer = packet.get();  //Destination IP 1st Octet
        destIP += buffer;
        destIP += ".";

        buffer = packet.get();  //Destination IP 2nd Octet
        destIP += buffer;
        destIP += ".";

        buffer = packet.get();  //Destination IP 3rd Octet
        destIP += buffer;
        destIP += ".";

        buffer = packet.get();  //Destination IP 4th Octet
        destIP += buffer;

        Log.d(TAG, "Destination IP:"+destIP);

        //status += "   Destination IP:"+destIP;

        int srcPort = packet.getChar();
        int dstPort = packet.getChar();
		Log.d(TAG, "srcPort: " + srcPort + " dstPort: " + dstPort);
        //Log.d(TAG, myIPpkt.toString());

	}
	
	
}
