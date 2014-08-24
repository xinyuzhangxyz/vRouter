package com.example.vrouter.util;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.util.Log;

public class HelpTools {
	private static final String TAG = "vRouterService_Tool";
	
    public static void reversePacketAddr(ByteBuffer templatePkt, 
    		IPPacket sentPkt, DatagramPacket receivedPacket) {
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
    	templatePkt.putChar(26, (char) calculateChecksum(myUDPpkt));
    	
    	templatePkt.put(myUDPpkt, 0, myUDPpkt.length);
    	templatePkt.put(receivedPacket.getData());
    	
    	templatePkt.rewind();
    	//printIPPacket(templatePkt);
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
    private static long calculateChecksum(byte[] buf) {
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
    
    
}
