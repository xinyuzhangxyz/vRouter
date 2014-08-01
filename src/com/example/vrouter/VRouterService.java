package com.example.vrouter;


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
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;

import android.os.AsyncTask;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class VRouterService extends VpnService implements Handler.Callback, Runnable {
    private static final String TAG = "VRouterService";

    private String mServerAddress = "127.0.0.1";//Local loopback server, for testing purpose only; emulating a VPN server
    private int mServerPort = 9040;
    private PendingIntent mConfigureIntent;

    private Handler mHandler;
    private Thread mThread;

    private ParcelFileDescriptor mInterface;

    //Intent UIupdateIntent = new Intent("UI_UPDATE");
    //i.putExtra("<Key>","text");
    //sendBroadcast(i);
    
    
    @Override
    // Will be called when this service is called as a new intent
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);// I handle callback by my own handleMessage()
        }

        // Stop the previous session by interrupting the thread.
        if (mThread != null) {
            mThread.interrupt();
        }
        // Start a new session by creating a new thread.
        mThread = new Thread(this, "vRouterThread");// I myself is a runnable with a run()
        mThread.start();
        
        //httpQuery hQ = new httpQuery();
		//hQ.execute("John");
		
        //  START_STICKY is used for services that are explicitly started and stopped as needed
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mThread != null) {
            mThread.interrupt();
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public synchronized void run() {
        try {
            Log.i(TAG, "Starting");

            // Set up server
            InetSocketAddress server = new InetSocketAddress(
                    mServerAddress, mServerPort);
            mHandler.sendEmptyMessage(R.string.connecting);
      
            run(server);
            
        } catch (Exception e) {
            Log.e(TAG, "Got " + e.toString());
            try {
                mInterface.close();
            } catch (Exception e2) {
                // ignore
            }
            mHandler.sendEmptyMessage(R.string.disconnected);

        } finally {
           
        }
    }
    /*
    @Override
    public synchronized void run() {
        try {
            Log.i(TAG, "Starting");

            // If anything needs to be obtained using the network, get it now.
            // This greatly reduces the complexity of seamless handover, which
            // tries to recreate the tunnel without shutting down everything.
            // In this demo, all we need to know is the server address.
            InetSocketAddress server = new InetSocketAddress(
                    mServerAddress, mServerPort);

            // We try to create the tunnel for several times. The better way
            // is to work with ConnectivityManager, such as trying only when
            // the network is avaiable. Here we just use a counter to keep
            // things simple.
            for (int attempt = 0; attempt < 10; ++attempt) {
                mHandler.sendEmptyMessage(R.string.connecting);

                // Reset the counter if we were connected.
                if (run(server)) {
                    attempt = 0;
                }

                // Sleep for a while. This also checks if we got interrupted.
                Thread.sleep(3000);
            }
            Log.i(TAG, "Giving up");
        } catch (Exception e) {
            Log.e(TAG, "Got " + e.toString());
        } finally {
            try {
                mInterface.close();
            } catch (Exception e) {
                // ignore
            }
            mInterface = null;

            mHandler.sendEmptyMessage(R.string.disconnected);
            Log.i(TAG, "Exiting");
        }
    }*/

    DatagramChannel mTunnel = null;


    private boolean run(InetSocketAddress server) throws Exception {
        boolean connected = false;
        
        android.os.Debug.waitForDebugger();
        
            // Create a DatagramChannel as the VPN tunnel.
        	mTunnel = DatagramChannel.open();
        	DatagramSocket dsk = mTunnel.socket();
        	if (dsk == null) {
        		Log.d(TAG, "ERROR! dsk is null!");
        	}
        	
            // Protect the tunnel before connecting to avoid loopback.
            if (!protect(dsk)) {
                throw new IllegalStateException("Cannot protect the local tunnel");
            }

            // Connect to the server.
            mTunnel.connect(server);

            // For simplicity, we use the same thread for both reading and
            // writing. Here we put the tunnel into non-blocking mode.
            mTunnel.configureBlocking(false);

            // Authenticate and configure the virtual network interface.
            handshake();

            // Now we are connected. Set the flag and show the message.
            connected = true;
            mHandler.sendEmptyMessage(R.string.connected);

            /*
            new Thread ()
            {
            	
            	public void run ()
            	{
            		// get server address and port
            		String serverName = "192.168.43.162";
            		InetAddress serverIPAddress = null;
            		try {
            			serverIPAddress = InetAddress.getByName(serverName);
            		} catch (UnknownHostException ex) {
            			Log.d(TAG, "Exception in TCP client thread.", ex);
            		}
            		
            		int serverPort = 12354;
            		// create socket which connects to server
            		try {
            			Socket clientSocket = new Socket(serverIPAddress, serverPort);
            			//Socket clientSocket = SocketChannel.open().socket();
                		//if ((null != clientSocket) && (null != this)) {
                		//	protect(clientSocket);
                		//}
                		//clientSocket.connect(new InetSocketAddress(serverIPAddress, serverPort), 10000);
                        String sentence = "A sentence sent to TCP server."; //inFromUser.readLine();
                        Log.d(TAG, "To Server: " + sentence);
                        // write to server
                        DataOutputStream outToServer = new
                            DataOutputStream(clientSocket.getOutputStream());
                        outToServer.writeBytes(sentence + '\n');
                        // create read stream and receive from server
                        BufferedReader inFromServer = new
                            BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        String sentenceFromServer = inFromServer.readLine();
                        Log.d(TAG, "From Server: " + sentenceFromServer);
                        // close client socket
                        clientSocket.close();
            		} catch(IOException ex) {
            			Log.d(TAG, "Exception in TCP client thread.", ex);
            		}
            	}
            }.start();
            */
            
            new Thread ()
            {
            	
            	public void run ()
            	{
            		DatagramChannel tunnel = mTunnel;

              	  	// Allocate the buffer for a single packet.
                    ByteBuffer packet = ByteBuffer.allocate(1024);
		            // Packets to be sent are queued in this input stream.
		            FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());
		            // Read the outgoing packet from the input stream.
		            int length;
		            
		            try
		            {
		            	httpQuery hQ = new httpQuery();
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
				                IPpkt pkt = new IPpkt();
				                ByteBuffer duplicateIPHeader = ByteBuffer.allocate(2048);
			                	debugPacket(packet, pkt, duplicateIPHeader);
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
			                		reversePacketAddr(duplicateIPHeader, pkt, receivePacket);
			                		
			                		
					                
					                duplicateIPHeader.limit(receivePacket.getLength() + 28);
					                Log.d(TAG, "print pkt to local tunnel total size: "+ duplicateIPHeader.limit());
					                printIPPacket(duplicateIPHeader);
			                		//tunnel.write(duplicateIPHeader);//send to VPN server
					                
					                
					                //write back to local VPN tunnel and to requester
					                Log.d(TAG, "Written to local requester");
					                duplicateIPHeader.rewind();
					                FileOutputStream out = new FileOutputStream(mInterface.getFileDescriptor());
						            out.write(duplicateIPHeader.array(), 0, receivePacket.getLength() + 28);
					                
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
		            }
		            
            	}
            }.start();
            
            
            new Thread ()
            {
            	
            	public void run ()
            	{
            		DatagramChannel tunnel = mTunnel;

              	  	// Allocate the buffer for a single packet.
                    ByteBuffer packet = ByteBuffer.allocate(8096);
		            // Packets received need to be written to this output stream.
		            FileOutputStream out = new FileOutputStream(mInterface.getFileDescriptor());
		            
		            while (true)
		            {
		                try
		                {
			                // Read the incoming packet from the tunnel.
			                int length;
			                while ((length = tunnel.read(packet)) > 0)
			                {
			                	Log.d(TAG, "Incoming packet written to the output stream.");
			                	IPpkt pkt = new IPpkt();
			                	ByteBuffer duplicateIPHeader = ByteBuffer.allocate(2048);
			                	debugPacket(packet, pkt, duplicateIPHeader);
			                	Log.d(TAG, pkt.toString());
			                	
			                    // Write the incoming packet to the output stream.
			                    out.write(packet.array(), 0, length);
			                    
			                    packet.clear();
			                }
		                }
		                catch (IOException ioe)
		                {
		                	ioe.printStackTrace();
		                }
	            	}
            	}
            }.start();

        return connected;
    }

    private void handshake() throws Exception {
       
    	if (mInterface == null)
    	{
	        Builder builder = new Builder();
	        
	        builder.setMtu(1400);
	        builder.addAddress("10.0.0.2",24);
	        //builder.addAddress("192.168.2.9",32);
	        builder.addRoute("0.0.0.0",0);
	        //builder.addDnsServer("8.8.8.8");
	       // builder.addSearchDomain("wisc.edu");
	        
	        // Close the old interface since the parameters have been changed.
	        try {
	            mInterface.close();
	        } catch (Exception e) {
	            // ignore
	        }
	        
	
	        // Create a new interface using the builder and save the parameters.
	        mInterface = builder.setSession(mServerAddress)
	                .setConfigureIntent(mConfigureIntent)
	                .establish();
    	}
    }

    /*
    public String getLocalIpAddress()
    {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                Log.i(TAG,"Inspect interface (human readable ): " + intf.getDisplayName());
                Log.i(TAG,"Inspect interface: " + intf.getDisplayName());
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    Log.i(TAG,"****** INET ADDRESS ******");
                    Log.i(TAG,"address: "+inetAddress.getHostAddress());
                    Log.i(TAG,"hostname: "+inetAddress.getHostName());
                    Log.i(TAG,"address.toString(): "+inetAddress.getHostAddress().toString());
                    if (!inetAddress.isLoopbackAddress()) {
                        //IPAddresses.setText(inetAddress.getHostAddress().toString());
                        Log.i(TAG,"IS NOT LOOPBACK ADDRESS: "+inetAddress.getHostAddress().toString());
                        return inetAddress.getHostAddress().toString();
                    } else{
                        Log.i(TAG,"It is a loopback address");
                    }
                }
            }
        } catch (SocketException ex) {
            String LOG_TAG = null;
            Log.e(LOG_TAG, ex.toString());
        }

        return null;
    }
    */
    

    
    private class IPpkt {
    	String srcIP;
    	String dstIP;
    	int srcPort;
    	int dstPort;
    	int pktType;//6:TCP  17:UDP
    	int pktLen; 
    	
    	@Override
    	public String toString() {
    		return "src="+srcIP+":"+srcPort+ "  dst="+dstIP+":"+dstPort + " len="+pktLen;
    	};
    }
    
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
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
    
    public void reversePacketAddr(ByteBuffer templatePkt, 
    		IPpkt sentPkt, DatagramPacket receivedPacket) {
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
    
    // Read packet header and extract useful information
    private void debugPacket(ByteBuffer packet, IPpkt myIPpkt, ByteBuffer dupHeader)
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
    
    // debugging purpose only
    private void printIPPacket(ByteBuffer packet)
    {
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
    public long calculateChecksum(byte[] buf) {
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

    // Send HTTP request to a server and get response
    protected class httpQuery extends AsyncTask<String, Void, String> {
		String reply = null;
		@Override
		protected String doInBackground(String... strs) {
			// Sample HTTP POST request code:
			String temp1="";
			HttpClient httpclient = new DefaultHttpClient();

				HttpPost getVal = new HttpPost("http://testdbserver.appspot.com/getvalue");
			
				// ArrayList<NameValuePair> is used to send values from android app to server.
		        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
		        
		        // "tag" is the name of the text form on the webserver
		        // "mytagInput" is the value that the client is submitting to the server
		        nameValuePairs.add(new BasicNameValuePair("tag", strs[0]));
		        
		      
				 try {
					 UrlEncodedFormEntity httpEntity = new UrlEncodedFormEntity(nameValuePairs);
					 getVal.setEntity(httpEntity); 
					 
					 HttpResponse response = httpclient.execute(getVal);
					 temp1 = EntityUtils.toString(response.getEntity());	
					} 
					  catch (ClientProtocolException e) {			  
						e.printStackTrace();
					} catch (IOException e) {
						System.out.println("HTTP IO Exception");
						e.printStackTrace();
					}
					 

		            // Decode the JSON array. Array is zero based so the return value is in element 2
					try {
						JSONArray jsonArray = new JSONArray(temp1);
						reply = jsonArray.getString(2);
						return reply;
					} catch (JSONException e) {
						System.out.println("Error in JSON decoding");
						e.printStackTrace();
					}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(String res) {
			//((TextView)findViewById(R.id.outVal)).setText("Temperature: "+res);
		}
	}
}
