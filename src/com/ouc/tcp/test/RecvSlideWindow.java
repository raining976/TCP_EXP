package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class RecvSlideWindow {
    private Client client;
    private LinkedList<TCP_PACKET> packets = new LinkedList<>(); // 暂存一下一个顺序的列表
    private int expectedSeq = 1; // 期待的seq
    Queue<int[]> dataQueue = new LinkedBlockingQueue<>(); // 数据列表

    public RecvSlideWindow(Client c){
        client = c;
    }

    public int recvPkt(TCP_PACKET pkt){
        int curSeq = pkt.getTcpH().getTh_seq();
        if(curSeq >= expectedSeq){
            putPkt(pkt);
        }

        slidWindow();
        return expectedSeq - 1;
    }

    public void putPkt(TCP_PACKET pkt){
        int curSeq = pkt.getTcpH().getTh_seq();

        int i = 0;
        // 找到合适位置（有序）插入到队列中
        for(;i < packets.size() && curSeq > packets.get(i).getTcpH().getTh_seq();i++);
        if (i == packets.size() || curSeq != packets.get(i).getTcpH().getTh_seq() ){
            packets.add(i,pkt);
        }
    }

    public void slidWindow(){
        // 将滑动窗口中的数据包移到数据队列中，直到窗口中的第一个数据包的序列号不再是期望序列号
        // 有序接收
        while(!packets.isEmpty() && packets.getFirst().getTcpH().getTh_seq() == expectedSeq){
            dataQueue.add(packets.poll().getTcpS().getData());
            expectedSeq++;
        }

        if(dataQueue.size() >= 20 && expectedSeq == 1000){
            deliver_data();
        }


    }

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
}
