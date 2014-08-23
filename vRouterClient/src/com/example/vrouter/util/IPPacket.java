package com.example.vrouter.util;

public class IPPacket {
	public String srcIP;
	public String dstIP;
	public int srcPort;
	public int dstPort;
	public int pktType;//6:TCP  17:UDP
	public int pktLen; 
	
	public String getSourceIP(){
		return srcIP;
	}
	
	public String getDestIP(){
		return dstIP;
	}
	
	public int getSourcePort(){
		return srcPort;
	}
	
	public int getDestPort(){
		return dstPort;
	}
	
	public int getPacketType(){
		return pktType;
	}
	
	public int getPacketLength(){
		return pktLen;
	}
	
	@Override
	public String toString() {
		return "src="+srcIP+":"+srcPort+ "  dst="+dstIP+":"+dstPort + " len="+pktLen;
	};
}
