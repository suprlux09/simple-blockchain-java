package org.example.threads;

import org.example.entities.Block;
import org.example.entities.Transaction;

import java.io.IOException;
import java.util.ArrayList;

import static org.example.App.*;
import static org.example.entities.BlockChain.*;
import static org.example.entities.Mempool.*;
import static org.example.utilities.WebRequest.*;

public class MinorThread extends Thread {
    @Override
    public void run() {
        while (true) {
            Block block = null;
            synchronized (lockBlockChain) {
                block = new Block(chain.get(chain.size()-1).hash);
            }

            if (block.process()) {
                try {
                    sendBroadcast(BLOCK_ENDPOINT, publicKeyList.keySet().toArray(new String[0]), block);
                    System.out.println("Block "+ block.hash +" has been mined successfully");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                System.out.println("Block "+ block.hash +" mining FAILED");
            }
        }
    }
}
