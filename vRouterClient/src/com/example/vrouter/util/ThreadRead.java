package com.example.vrouter.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import android.os.ParcelFileDescriptor;
import android.util.Log;

public class ThreadRead implements Runnable{
	
	private static final String TAG = "ThreadRead";
	private DatagramChannel mTunnel;
	private ParcelFileDescriptor mInterface;
	
	
	public ThreadRead(DatagramChannel t, ParcelFileDescriptor i){
		mTunnel = t;
		mInterface = i;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
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
                	IPPacket pkt = new IPPacket();
                	ByteBuffer duplicateIPHeader = ByteBuffer.allocate(2048);
                	HelpTools.debugPacket(packet, pkt, duplicateIPHeader);
                	Log.d(TAG, pkt.toString());
                	
                    // Write the incoming packet to the output stream.
                    out.write(packet.array(), 0, length);
                    
                    packet.clear();
                }
            }
            catch (IOException ioe)
            {
            	ioe.printStackTrace();
            }finally{
            	if(out!= null)
					try {
						out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            }
    	}
	}
	
}
