package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Queue;
import java.util.TimerTask;

public class RetransmitTask extends TimerTask {
    Client sendClient;
    Queue<TCP_PACKET> packets;
    RetransmitTask(Client client, Queue<TCP_PACKET> packets){
        sendClient = client;
        this.packets = packets;
    }

    @Override
    public void run(){
        for(TCP_PACKET p:packets){
            sendClient.send(p);
        }
    }
}
