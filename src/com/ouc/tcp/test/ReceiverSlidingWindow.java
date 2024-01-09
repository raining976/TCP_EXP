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

public class ReceiverSlidingWindow {
    private Client client;
    private LinkedList<TCP_PACKET> packets = new LinkedList<>();
    private int expectedSequence = 1;
    Queue<int[]> dataQueue = new LinkedBlockingQueue();

    public ReceiverSlidingWindow(Client client) {
        this.client = client;
    }

    /**
     * 接收 TCP 数据包并处理
     * @param packet 收到的 TCP 数据包
     * @return 返回已处理的数据包的期望序列号
     */
    public int receivePacket(TCP_PACKET packet) {
        int currentSequence = packet.getTcpH().getTh_seq();

        // 如果当前数据包序列号大于等于期望序列号，则将其放入接收队列并进行滑动窗口处理
        if (currentSequence >= this.expectedSequence) {
            putPacket(packet);
        }

        // 执行滑动窗口操作
        slid();

        // 返回已处理的数据包的期望序列号
        return this.expectedSequence - 1;
    }

    /**
     * 将数据包按序插入接收队列
     * @param packet 要插入的数据包
     */
    private void putPacket(TCP_PACKET packet) {
        int currentSequence = packet.getTcpH().getTh_seq();

        // 找到合适的位置插入数据包，保持队列有序
        int index = 0;
        while (index < this.packets.size()
                && currentSequence > this.packets.get(index).getTcpH().getTh_seq()) {
            index++;
        }

        // 如果数据包不在队列中，则插入到指定位置
        if (index == this.packets.size()
                || currentSequence != this.packets.get(index).getTcpH().getTh_seq()) {
            this.packets.add(index, packet);
        }
    }

    /**
     * 执行滑动窗口操作，将滑动窗口中的数据包移到数据队列中
     */
    private void slid() {
        // 将滑动窗口中的数据包移到数据队列中，直到窗口中的第一个数据包的序列号不再是期望序列号
        while (!this.packets.isEmpty()
                && this.packets.getFirst().getTcpH().getTh_seq()== this.expectedSequence) {
            this.dataQueue.add(this.packets.poll().getTcpS().getData());
            this.expectedSequence++;
        }

        // 如果数据队列达到一定大小或期望序列号达到最大值，执行数据交付操作
        if (this.dataQueue.size() >= 20 || this.expectedSequence == 1000) {
            this.deliver_data();
        }
    }

    /**
     * 数据交付操作：将数据写入文件
     */
    public void deliver_data() {
        // 检查 this.dataQueue，将数据写入文件
        try {
            File file = new File("recvData.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

            while (!this.dataQueue.isEmpty()) {
                int[] data = this.dataQueue.poll();

                // 将数据写入文件
                for (int i = 0; i < data.length; i++) {
                    writer.write(data[i] + "\n");
                }

                writer.flush();  // 清空输出缓存
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
