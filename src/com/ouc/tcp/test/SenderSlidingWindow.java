package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Hashtable;
import java.util.TimerTask;
import java.util.*;

public class SenderSlidingWindow {
    private Client client;
    public int cwnd = 1;  // 拥塞窗口大小
    private volatile int ssthresh = 16;  // 慢启动阈值
    private int count = 0;  // 拥塞避免：cwmd = cwmd + 1 / cwnd，每一个对新包的 ACK count++，所以 count == cwmd 时，cwnd = cwnd + 1
    private Hashtable<Integer, TCP_PACKET> packets = new Hashtable<>();  // 存储已发送但未收到确认的数据包
    private UDT_Timer timer;
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

        if(timer == null){
            timer = new UDT_Timer();
            this.timer.schedule(new RetransmitTask(this),3000,3000);
        }
    }

    // 处理接收到的 ACK
    public void receiveACK(int currentSequence) {
        if (currentSequence == this.lastACKSequence) {
            this.lastACKSequenceCount++;
            if (this.lastACKSequenceCount == 4) {
                TCP_PACKET packet = this.packets.get(currentSequence + 1);
                if (packet != null) {
                    this.client.send(packet);
                    if(timer != null) timer.cancel();
                    timer = new UDT_Timer();
                    timer.schedule(new RetransmitTask(this),3000,3000);

                }

                fastRecovery();
            }
        } else {
            List sequenceList = new ArrayList(this.packets.keySet());
            Collections.sort(sequenceList);
            for (int i = 0; i < sequenceList.size() && (int) sequenceList.get(i) <= currentSequence; i++) {
                this.packets.remove(sequenceList.get(i));
            }

            if (this.timer != null) {
                this.timer.cancel();
            }

            if (this.packets.size() != 0) {
                this.timer = new UDT_Timer();
                this.timer.schedule(new RetransmitTask(this), 3000, 300);
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
        this.ssthresh = this.cwnd / 2;
        if (this.ssthresh < 2) {
            this.ssthresh = 2;
        }
        this.cwnd = 1;
    }

    public void fastRecovery() {
        this.ssthresh = this.cwnd / 2;
        if (this.ssthresh < 2) {
            this.ssthresh = 2;
        }
        this.cwnd = this.ssthresh;
    }

    public void retransmit(){
        this.timer.cancel();

        List sequenceList = new ArrayList(this.packets.keySet());
        Collections.sort(sequenceList);

        for (int i = 0; i < this.cwnd && i < sequenceList.size(); i++) {
            TCP_PACKET packet = this.packets.get(sequenceList.get(i));
            if (packet != null) {
                System.out.println("retransmit: " + (packet.getTcpH().getTh_seq() - 1) / 100);
                this.client.send(packet);
            }
        }

        if (this.packets.size() != 0) {
            this.timer = new UDT_Timer();
            this.timer.schedule(new RetransmitTask(this), 3000, 3000);
        } else {
            System.out.println("000000000000000000 no packet");
        }
    }
}


