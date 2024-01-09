
package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Hashtable;
import java.util.TimerTask;

public class SenderSlidingWindow {
    private Client client;
    public int cwnd = 1;  // 拥塞窗口大小
    private volatile int ssthresh = 16;  // 慢启动阈值
    private int count = 0;  // 拥塞避免：cwmd = cwmd + 1 / cwnd，每一个对新包的 ACK count++，所以 count == cwmd 时，cwnd = cwnd + 1
    private Hashtable<Integer, TCP_PACKET> packets = new Hashtable<>();  // 存储已发送但未收到确认的数据包
    private Hashtable<Integer, UDT_Timer> timers = new Hashtable<>();  // 存储每个数据包的定时器
    private int lastACKSequence = -1;  // 最后收到的 ACK 序列号
    private int lastACKSequenceCount = 0;  // 连续相同 ACK 的次数

    public SenderSlidingWindow(Client client) {
        this.client = client;
    }

    // 判断滑动窗口是否已满
    public boolean isFull() {
        return this.cwnd <= this.packets.size();
    }

    // 将数据包放入滑动窗口
    public void putPacket(TCP_PACKET packet) {
        int currentSequence = packet.getTcpH().getTh_seq();  // 数据包序列号
        this.packets.put(currentSequence, packet);
        this.timers.put(currentSequence, new UDT_Timer());
        this.timers.get(currentSequence).schedule(new RetransmitTask(this.client, packet, this), 3000, 3000);  // 设置数据包的定时器
    }

    // 处理接收到的 ACK
    public void receiveACK(int currentSequence) {
        if (currentSequence == this.lastACKSequence) {
            this.lastACKSequenceCount++;
            if (this.lastACKSequenceCount == 4) {
                TCP_PACKET packet = this.packets.get(currentSequence + 1);
                if (packet != null) {
                    this.client.send(packet);
                    this.timers.get(currentSequence + 1).cancel();
                    this.timers.put(currentSequence + 1, new UDT_Timer());
                    this.timers.get(currentSequence + 1).schedule(new RetransmitTask(this.client, packet, this), 3000, 3000);
                }

                slowStart();  // 连续4次相同 ACK 触发慢启动
            }
        } else {
            // 处理连续的 ACK
            for (int i = this.lastACKSequence + 1; i <= currentSequence; i++) {
                this.packets.remove(i);

                if (this.timers.containsKey(i)) {
                    this.timers.get(i).cancel();
                    this.timers.remove(i);
                }
            }

            this.lastACKSequence = currentSequence;
            this.lastACKSequenceCount = 1;

            if (this.cwnd < this.ssthresh) {
                this.cwnd++;
                System.out.println("########### 窗口扩大 ############");
            } else {
                this.count++;
                if (this.count >= this.cwnd) {
                    this.count -= this.cwnd;
                    this.cwnd++;
                    System.out.println("########### 窗口扩大 ############");
                }
            }
        }
    }

    // 慢启动算法
    public void slowStart() {
        System.out.println("00000 cwnd: " + this.cwnd);
        System.out.println("00000 ssthresh: " + this.ssthresh);
        this.ssthresh = this.cwnd / 2;
        this.cwnd = 1;
        System.out.println("11111 cwnd: " + this.cwnd);
        System.out.println("11111 ssthresh: " + this.ssthresh);
    }
}

