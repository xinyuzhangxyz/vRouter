package com.example.vrouter.util;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.app.PendingIntent;
import android.net.VpnService.Builder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class RoutineTools {

	private final static String TAG = "VRouterService_Rountines";
	
    public static void handshake(Builder builder, ParcelFileDescriptor mInterface, String mServerAddress, PendingIntent mConfigureIntent) throws Exception {
        
    	if (mInterface == null)
    	{
//	        VpnService.Builder builder = new Builder();
	        
	        builder.setMtu(1400);
	        builder.addAddress("10.0.0.2",24);
	        //builder.addAddress("192.168.2.9",32);
	        builder.addRoute("0.0.0.0",0);
	        //builder.addDnsServer("8.8.8.8");
	       // builder.addSearchDomain("wisc.edu");
	        
	        // Close the old interface since the parameters have been changed.
	        
	        // this part of code is useless, since it only execute when mInterface is null because it is in the 
	        // if(mInterface == null)
//	        try {
//	            mInterface.close();
//	        } catch (NullPointerException e) {
//	            // ignore
//	        }
	        
	
	        // Create a new interface using the builder and save the parameters.
	        mInterface = builder.setSession(mServerAddress)
	                .setConfigureIntent(mConfigureIntent)
	                .establish();
    	}
    }
	
	
	
	
    /**
     * Calculate the Internet Checksum of a buffer (RFC 1071 - http://www.faqs.org/rfcs/rfc1071.html)
     * Algorithm is
     * 1) apply a 16-bit 1's complement sum over all octets (adjacent 8-bit pairs [A,B], final odd length is [A,0])
     * 2) apply 1's complement to this final sum
     *
     * Notes:
     * 1's complement is bitwise NOT of positive value.
     * Ensure that any carry bits are added back to avoid off-by-one errors
     *
     *
     * @param buf The message
     * @return The checksum
     */
    public static long calculateChecksum(byte[] buf) {
      int length = buf.length;
      int i = 0;

      long sum = 0;
      long data;

      // Handle all pairs
      while (length > 1) {
        // Corrected to include @Andy's edits and various comments on Stack Overflow
        data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
        sum += data;
        // 1's complement carry bit correction in 16-bits (detecting sign extension)
        if ((sum & 0xFFFF0000) > 0) {
          sum = sum & 0xFFFF;
          sum += 1;
        }

        i += 2;
        length -= 2;
      }

      // Handle remaining byte in odd length buffers
      if (length > 0) {
        // Corrected to include @Andy's edits and various comments on Stack Overflow
        sum += (buf[i] << 8 & 0xFF00);
        // 1's complement carry bit correction in 16-bits (detecting sign extension)
        if ((sum & 0xFFFF0000) > 0) {
          sum = sum & 0xFFFF;
          sum += 1;
        }
      }

      // Final 1's complement value correction to 16-bits
      sum = ~sum;
      sum = sum & 0xFFFF;
      return sum;
    }
	
    public static void reversePacketAddr(ByteBuffer templatePkt, IPPacket sentPkt, DatagramPacket receivedPacket) {
    	// Regenerate IP header and header checksum
    	//myPkt.put(templatePkt);
    	templatePkt.putChar(10, (char)0);//Initialize header checksum to 0
    	byte[] srcIP = receivedPacket.getAddress().getAddress();
    	byte[] dstIP = null;
    	try {
    		//InetAddress myip = InetAddress.getByName(sentPkt.srcIP);
    		dstIP = InetAddress.getByName(sentPkt.srcIP).getAddress();
    	} catch (UnknownHostException ex) {
    		Log.d(TAG, "In reversePacketAddr()", ex);
    	}
    	//dstIP[0] = (byte)127;//test only
    	//dstIP[1] = (byte)0;
    	//dstIP[2] = (byte)0;
    	//dstIP[3] = (byte)1;
    	
    	for (int b = 12; b < 16; b ++) {
    		templatePkt.put(b, srcIP[b-12]);
    	}
    	for (int b = 16; b < 20; b ++) {
    		templatePkt.put(b, dstIP[b-16]);
    	}
    	
    	byte [] IPheader = new byte[20];
    	templatePkt.put(IPheader, 0, 20);
    	templatePkt.putChar(10, (char) calculateChecksum(IPheader));
    	
    	// Regenerate UDP header and checksum
    	int srcPort = receivedPacket.getPort();
    	int dstPort = sentPkt.srcPort;
    	int UDPdataLen = receivedPacket.getLength();
    	
    	templatePkt.putChar(20, (char)srcPort);
    	templatePkt.putChar(22, (char)dstPort);
    	templatePkt.putChar(24, (char)UDPdataLen);
    	//Log.d(TAG, "reversed srcIP: "+);
    	//Log.d(TAG, "reversed srcPort: "+srcPort+" dstPort: "+dstPort+" UDPdataLen: "+UDPdataLen);
    	
    	byte[] myUDPpkt = new byte[8+UDPdataLen];
    	
    	// Note: UDP checksum is for both the header AND data
    	myUDPpkt[0] = (byte) ((srcPort&0xFF00) >> 8);
    	myUDPpkt[1] = (byte) (srcPort&0x00FF);
    	myUDPpkt[2] = (byte) ((dstPort&0xFF00) >> 8);
    	myUDPpkt[3] = (byte) (dstPort&0x00FF);
    	myUDPpkt[4] = (byte) ((UDPdataLen&0xFF00) >> 8);
    	myUDPpkt[5] = (byte) (UDPdataLen&0x00FF);
    	myUDPpkt[6] = (byte) 0;
    	myUDPpkt[7] = (byte) 0;
    	System.arraycopy(receivedPacket.getData(), 0, myUDPpkt, 8, UDPdataLen);
    	templatePkt.putChar(26, (char)calculateChecksum(myUDPpkt));
    	
    	templatePkt.put(myUDPpkt, 0, myUDPpkt.length);
    	templatePkt.put(receivedPacket.getData());
    	
    	templatePkt.rewind();
    	//printIPPacket(templatePkt);
    }
    
    /**
     * Read packet header and extract useful information
     * @param packet
     * @param myIPpkt
     * @param dupHeader
     */
    
    public static void debugPacket(ByteBuffer packet, IPPacket myIPpkt, ByteBuffer dupHeader)
    {
    	// Reference: http://en.wikipedia.org/wiki/IPv4
		//Log.d(TAG, "Original bytes: " + bytesToHex(packet.array()));
		
        int buffer = packet.get();
        int version;
        int headerlength;
        int protocol;
        version = buffer >> 4;
        headerlength = buffer & 0x0F;
        dupHeader.put((byte)buffer);
        
        headerlength *= 4;
        //Log.d(TAG, "IP Version:"+version);
        //Log.d(TAG, "Header Length:"+headerlength);

        String status = "";
        //status += "Header Length:"+headerlength;

        buffer = packet.get();      //DSCP + EN
        dupHeader.put((byte)buffer);
        buffer = packet.getChar();  //Total Length
        dupHeader.putChar((char)buffer);
        myIPpkt.pktLen = buffer;
        
        //Log.d(TAG, "Total Length:"+buffer);

        buffer = packet.getChar();  //Identification
        dupHeader.putChar((char)buffer);
        buffer = packet.getChar();  //Flags + Fragment Offset
        dupHeader.putChar((char)buffer);
        buffer = packet.get();      //Time to Live
        dupHeader.put((byte)buffer);
        buffer = packet.get();      //Protocol
        protocol = buffer;
        dupHeader.put((byte)buffer);
        
        //Log.d(TAG, "Protocol:"+buffer);

        status += "  Protocol:"+buffer;
        myIPpkt.pktType = buffer;
        
        buffer = packet.getChar();  //Header checksum
        dupHeader.putChar((char)buffer);
        
        String sourceIP  = "";
        buffer = (int) (packet.get()&0x00FF);  //Source IP 1st Octet FIXME
        dupHeader.put((byte)buffer);
        sourceIP += buffer;
        sourceIP += ".";

        buffer = (int) (packet.get()&0x00FF);//packet.get();  //Source IP 2nd Octet
        dupHeader.put((byte)buffer);
        sourceIP += buffer;
        sourceIP += ".";

        buffer = (int) (packet.get()&0x00FF);//packet.get();  //Source IP 3rd Octet
        dupHeader.put((byte)buffer);
        sourceIP += buffer;
        sourceIP += ".";

        buffer = (int) (packet.get()&0x00FF);//packet.get();  //Source IP 4th Octet
        dupHeader.put((byte)buffer);
        sourceIP += buffer;

        //Log.d(TAG, "Source IP:"+sourceIP);

        //status += "   Source IP:"+sourceIP;

        String destIP  = "";
        buffer = (int) (packet.get()&0x00FF);//packet.get();  //Destination IP 1st Octet
        dupHeader.put((byte)buffer);
        destIP += buffer;
        destIP += ".";

        buffer = (int) (packet.get()&0x00FF);//packet.get();  //Destination IP 2nd Octet
        dupHeader.put((byte)buffer);
        destIP += buffer;
        destIP += ".";

        buffer = (int) (packet.get()&0x00FF);//packet.get();  //Destination IP 3rd Octet
        dupHeader.put((byte)buffer);
        destIP += buffer;
        destIP += ".";

        buffer = (int) (packet.get()&0x00FF);//packet.get();  //Destination IP 4th Octet
        dupHeader.put((byte)buffer);
        destIP += buffer;

        //Log.d(TAG, "Destination IP:"+destIP);

        //status += "   Destination IP:"+destIP;

		//Log.d(TAG, "RX bytes: " + bytesToHex(packet.array()));

        myIPpkt.srcIP = sourceIP;
        myIPpkt.dstIP = destIP;
    	myIPpkt.srcPort = packet.getChar();
    	myIPpkt.dstPort = packet.getChar();
        if (protocol == 17) {//UDP
        	int len = packet.getChar();
        	int checksum = packet.getChar();//UDP checksum
        } else if (protocol ==6) {//TCP
        	int seq = packet.getInt();
        	int ACKseq = packet.getInt();
        	int data_resv = packet.get();
        	int flags = packet.get();
        	int window = packet.getChar();
        	int checksum = packet.getChar();
        	int urgent = packet.getChar();
        }
        //Log.d(TAG, myIPpkt.toString());
        
        //Log.d(TAG, "duplicated IP packet.");
        //printIPPacket(dupHeader);
    }

    public static String bytesToHex(byte[] bytes, char[] hexArray){
        //char[] hexChars = new char[bytes.length * 2];
        //for ( int j = 0; j < bytes.length; j++ ) {
    	char[] hexChars = new char[200];
        for ( int j = 0; j < 100; j++ ) {
            int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    
    
    
}
