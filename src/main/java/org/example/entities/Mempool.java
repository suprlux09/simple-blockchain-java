package org.example.entities;

import java.util.ArrayList;

public class Mempool {
    public static ArrayList<Transaction> pool = new ArrayList<>();
    public static Object lockMempool = new Object();

    public static void purgeMempool(Block block) {
        for (Transaction transaction : block.transactions) {
            pool.remove(transaction);
        }
    }
}
