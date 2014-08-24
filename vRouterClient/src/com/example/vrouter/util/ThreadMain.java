package com.example.vrouter.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import android.os.ParcelFileDescriptor;
import android.util.Log;

public class ThreadMain implements Runnable{

	private static final String TAG = "ThreadMain";
	private ParcelFileDescriptor mInterface;
	
	
	public ThreadMain(){
		
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
        ByteBuffer packet = ByteBuffer.allocate(1024);
        // Packets to be sent are queued in this input stream.
        FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());
        // Read the outgoing packet from the input stream.
        int length;
        
        try
        {
        	HttpQuery hQ = new HttpQuery();
    		hQ.execute("12345678901234567890");
        	
        	while (true)
        	{
	            while ((length = in.read(packet.array())) > 0) {
	                // Write the outgoing packet to the tunnel.
	                packet.limit(length);
	                
	                Log.d(TAG, "Outgoing packet written to the tunnel");
	                //debugPacket(packet);
                    
	                /*
	                1. Parse the TCP/UDP header
	                2. Create an own socket with the same src/dest port/ip
	                3. Use protect() on this socket so it is not routed over tun0
	                4. Send the packet body (excluding the header)
	                5. Obtain the response
	                6. Add the TCP header to the response and forward it
	                */
	                IPPacket pkt = new IPPacket();
	                ByteBuffer duplicateIPHeader = ByteBuffer.allocate(2048);
//                	debugPacket(packet, pkt, duplicateIPHeader);
                	Log.d(TAG, "Pkt info: "+pkt.toString());
                	
                	if (pkt.pktType == 6) { //TCP
                		Log.d(TAG, "got TCP packet!!!");
                		Socket socket = SocketChannel.open().socket();
                		if ((null != socket) && (null != this)) {
                			protect(socket);
                		}
                		Log.d(TAG, "start connection.");
	                	socket.connect(new InetSocketAddress(pkt.dstIP, pkt.dstPort), 1000);
	                
                		Log.d(TAG, "send out TCP pkt size "  + packet.remaining());
	                	DataOutputStream outToServer = new
	                		DataOutputStream(socket.getOutputStream());
	                
	                	byte[] pktBuf = new byte[packet.remaining()];
                		Arrays.fill(pktBuf, (byte)0);
                		packet.get(pktBuf, 0, packet.remaining());
	                	outToServer.write(pktBuf);
	                	
	                	BufferedReader inFromServer = new
	                			BufferedReader(new InputStreamReader(socket.getInputStream()));
	                	String sentenceFromServer = inFromServer.readLine();
	                	Log.d(TAG, "From Server: " + sentenceFromServer);

                	} else if (pkt.pktType == 17) { //UDP
                		DatagramSocket socket = DatagramChannel.open().socket();
                		if ((null != socket) && (null != this)) {
                			protect(socket);
                		}
                		
                		//Must bind this socket to a port and our local IP;
                		//Otherwise a random unknown port will be assigned
                		try {
                			socket.bind(new 
                					InetSocketAddress(InetAddress.getByName("192.168.43.129"), 33337));
                					//InetSocketAddress(InetAddress.getByName("192.168.2.10"), 33337));
                		} catch (IllegalArgumentException ex) {
                			Log.d(TAG, "bind exception. ", ex);
                		} catch (SocketException ex) {
                			Log.d(TAG, "bind exception. ", ex);
                		}
                		
                		/*try {
                			socket.setSoTimeout(1000);
                			socket.connect(new InetSocketAddress(pkt.dstIP, pkt.dstPort));
                		} catch (SocketException se) {
                			se.printStackTrace();
                		}*/
                		
                		//ByteBuffer pktBuf = packet.wrap(packet.array(), 20, pkt.pktLen-20);
                		byte[] pktBuf = new byte[500];
                		Arrays.fill(pktBuf, (byte)0);
                		//Log.d(TAG, "prepare to read "+(pkt.pktLen-28) + ";" + packet.remaining());
                		packet.get(pktBuf, 0, packet.remaining());
                		DatagramPacket outPacket = new DatagramPacket(
                				pktBuf,
                				pkt.pktLen-28, 
                				InetAddress.getByName(pkt.dstIP), pkt.dstPort);
                		//Log.d(TAG, "TX bytes: " + bytesToHex(pktBuf));
                		socket.send(outPacket);
                		Log.d(TAG, "sent UDP pkt size " + outPacket.getLength());

                		
                		
                		if (socket.isBound()) {
                			Log.d(TAG, "UDP socket bound to " 
                				+ socket.getLocalAddress().toString()+":"
                				+ socket.getLocalPort());
                		}
                		Arrays.fill(pktBuf, (byte)0);
                		DatagramPacket receivePacket =
                				new DatagramPacket(pktBuf, pktBuf.length);
                		try {
                			socket.receive(receivePacket);
                		} catch (IOException ioe) {
                			ioe.printStackTrace();
                			Log.d(TAG, "receive socket exception. ", ioe);
                		}
                		//Log.d(TAG, "RX data length="+receivePacket.getLength()
                		//		+ " data: " + bytesToHex(receivePacket.getData()));
                		
                		/*
                		DatagramPacket outPacket1 = new DatagramPacket(
                				receivePacket.getData(),
                				receivePacket.getLength(), 
                				InetAddress.getByName(pkt.srcIP), pkt.srcPort);
                		//Log.d(TAG, "TX bytes: " + bytesToHex(pktBuf));
                		socket.send(outPacket1);
                		Log.d(TAG, "sent UDP pkt back to local VPN source, size " 
                					+ outPacket1.getLength());
                		*/
                		
                		//Write IP packet to local VPN source socket
                		//Note: cannot use UDP socket to send; need to use TUN
                		//packet.clear();
                		//packet.put(receivePacket.getData(), 0, receivePacket.getLength());
                		// reverse the src and dst of this packet and return it back to 
                		// VPN source address
                		HelpTools.reversePacketAddr(duplicateIPHeader, pkt, receivePacket);
                		
                		
		                
		                duplicateIPHeader.limit(receivePacket.getLength() + 28);
		                Log.d(TAG, "print pkt to local tunnel total size: "+ duplicateIPHeader.limit());
//		                printIPPacket(duplicateIPHeader);
                		//tunnel.write(duplicateIPHeader);//send to VPN server
		                
		                
		                //write back to local VPN tunnel and to requester
		                Log.d(TAG, "Written to local requester");
		                duplicateIPHeader.rewind();
		                FileOutputStream out = new FileOutputStream(mInterface.getFileDescriptor());
			            out.write(duplicateIPHeader.array(), 0, receivePacket.getLength() + 28);
		                
			            out.close();
                	} else {
                		Log.d(TAG, "Wrong packet type " + pkt.pktType);
                	}
                	
	                packet.clear();
	
	            }
        	}
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }finally{
        	
        }
        
	}


}
