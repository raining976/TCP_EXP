package com.ouc.tcp.test;

import java.util.zip.CRC32;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

public class CheckSum {
	
	/*计算TCP报文段校验和：只需校验TCP首部中的seq、ack和sum，以及TCP数据字段*/
	public static short computeChkSum(TCP_PACKET tcpPack) {
		CRC32 crc32 = new CRC32();
		
		TCP_HEADER  header = tcpPack.getTcpH(); // 获取tcp头部
		crc32.update(header.getTh_seq()); // 计算seq
		crc32.update(header.getTh_ack()); // 计算ack
		
		for(int i = 0; i < tcpPack.getTcpS().getData().length; i++) {
			crc32.update(tcpPack.getTcpS().getData()[i]); // 计算数据部分
		}
		
		return (short) crc32.getValue();
	}
	
}
