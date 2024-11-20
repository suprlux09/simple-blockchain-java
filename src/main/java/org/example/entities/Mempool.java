package org.example.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Mempool {
    public static final List<Transaction> pool = Collections.synchronizedList(new ArrayList<>());

    public static void purgeMempool(Block block) {
        for (Transaction transaction : block.transactions) {
            pool.remove(transaction);
        }
    }
}
