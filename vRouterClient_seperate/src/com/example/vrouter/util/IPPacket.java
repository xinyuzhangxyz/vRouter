package com.example.vrouter.util;

public class IPPacket {
	public String srcIP;
	public String dstIP;
	public int srcPort;
	public int dstPort;
	public int pktType;//6:TCP  17:UDP
	public int pktLen; 
	
	@Override
	public String toString() {
		return "src="+srcIP+":"+srcPort+ "  dst="+dstIP+":"+dstPort + " len="+pktLen;
	};
}
