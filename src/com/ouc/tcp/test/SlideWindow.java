package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.concurrent.LinkedBlockingQueue;

public class SlideWindow {
    private Client sendClient;
    private final int windowSize = 16;
    private int send_base = 1;
    private int next_seq = 1;
    private UDT_Timer timer ;
    private RetransmitTask task;
    private LinkedBlockingQueue<TCP_PACKET> packets;

    private final int delay = 3000;
    SlideWindow(Client client){
        sendClient = client;
        timer = new UDT_Timer();
        packets = new LinkedBlockingQueue<>();
    }

    void addPacket(TCP_PACKET packet) throws CloneNotSupportedException {
        if(isFull()) return;
        packets.offer(packet.clone());
        if(send_base == next_seq){
            timer = new UDT_Timer();
            task = new RetransmitTask(sendClient,packets);
            timer.schedule(task,delay,delay);
        }
        next_seq++;
    }

    void receiveAck(int ackSeq){
        if(ackSeq >= send_base && ackSeq < send_base + windowSize){
            int delta = ackSeq + 1 - send_base;
            send_base = ackSeq + 1;
            slideWnd(delta);
            timer.cancel();
            if(send_base != next_seq){
                timer = new UDT_Timer();
                task = new RetransmitTask(sendClient,packets);
                timer.schedule(task,delay,delay);
            }

        }
    }

    void slideWnd(int d){
        for(int i = 0 ; i < d; i++){
            packets.poll();
        }
    }

    boolean isFull(){
        return packets.size() >= windowSize;
    }





}
