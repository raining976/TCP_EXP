package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Queue;
import java.util.TimerTask;

public class RetransmitTask extends TimerTask {
    Client sendClient;
    TCP_PACKET packet;
    SenderSlidingWindow window;
    RetransmitTask(SenderSlidingWindow window){
        this.window = window;
    }

    @Override
    public void run(){
        window.slowStart();

        window.retransmit();
    }
}
