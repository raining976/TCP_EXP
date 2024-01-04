/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Receiver extends TCP_Receiver_ADT {

    private TCP_PACKET ackPack;    //回复的ACK报文段
    private int expectedSeq = 1;

    /*构造函数*/
    public TCP_Receiver() {
        super();    //调用超类构造函数
        super.initTCP_Receiver(this);    //初始化TCP接收端
    }

    @Override
    //接收到数据报：检查校验和，设置回复的ACK报文段
    public void rdt_recv(TCP_PACKET recvPack) {
        //检查校验码，生成ACK
        if (CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
            int recvSeq = recvPack.getTcpH().getTh_seq();
            if (expectedSeq == recvSeq) {
                //生成ACK报文段（设置确认号）
                tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
                ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
                tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
                //回复ACK报文段
                reply(ackPack);

                expectedSeq++;
                dataQueue.add(recvPack.getTcpS().getData());
                //交付数据（每20组数据交付一次）
                if (dataQueue.size() == 20)
                    deliver_data();
            }
            else {
                System.out.println("Recieve Computed: " + CheckSum.computeChkSum(recvPack));
                System.out.println("Recieved Packet" + recvPack.getTcpH().getTh_sum());
                tcpH.setTh_ack(expectedSeq - 1);  // 将ack设置为上次接收成功的数据序号起始点
                ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
                tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
                //回复ACK报文段
                reply(ackPack);
            }
        }


    }


    @Override
    //交付数据（将数据写入文件）；不需要修改
    public void deliver_data() {
        //检查dataQueue，将数据写入文件
        File fw = new File("recvData.txt");
        BufferedWriter writer;

        try {
            writer = new BufferedWriter(new FileWriter(fw, true));

            //循环检查data队列中是否有新交付数据
            while (!dataQueue.isEmpty()) {
                int[] data = dataQueue.poll();

                //将数据写入文件
                for (int i = 0; i < data.length; i++) {
                    writer.write(data[i] + "\n");
                }

                writer.flush();        //清空输出缓存
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    //回复ACK报文段
    public void reply(TCP_PACKET replyPack) {
        //设置错误控制标志
        /**
         * 0 信道无差错
         * 1 只出错
         * 2 只丢包
         * 3 只延迟
         * 4 出错/丢包
         * 5 出错/延迟
         * 6 丢包/延迟
         * 7 出错/丢包/延迟
         */
        tcpH.setTh_eflag((byte) 7);    //eFlag=0，信道无错误

        //发送数据报
        client.send(replyPack);
    }

}
