package org.example;

import java.util.ArrayList;
import java.util.Date;

import static org.example.App.difficulty;
import static org.example.App.mempool;
import static org.example.Utility.applySHA256;

public class Block {

    public String hash;
    public String previousHash;
    private String merkleRoot;
    public ArrayList<Transaction> transactions = new ArrayList<>();
    private long timeStamp;
    private int nonce;

    public Block(String previousHash) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash();
    }

    public String calculateHash() {
        return applySHA256(previousHash + merkleRoot + Long.toString(timeStamp) + Integer.toString(nonce));
    }

    public void mineBlock(int difficulty) {
        this.merkleRoot = Utility.getMerkleRoot(transactions);
        String target = "0".repeat(difficulty);
        while(!hash.substring(0, difficulty).equals(target)) {
            nonce += 1;
            hash = calculateHash();
        }
        System.out.println("Nonce: " + Integer.toString(nonce));
        System.out.println("Hash " + hash);
    }

    /**
     * 블록 검증
     * <p>
     * 해시값, mempool에 모든 트랜잭션이 존재하는지 검증
     * <p>
     * 아직 블록체인에 포함되지 않았으면서, 블록체인에 포함될 수 있는지 여부를 검증
     *
     * @return 유효하면 true, 유효하지 않으면 false
     */
    public boolean validateBlock() {
        //compare registered hash and calculated hash:
        if(!this.hash.equals(this.calculateHash()) ){
            System.out.println("#Current Hashes not equal");
            return false;
        }
        //check if hash is solved
        if(!this.hash.substring(0, difficulty).equals("0".repeat(difficulty))) {
            System.out.println("#This block hasn't been mined");
            return false;
        }

        for (Transaction transaction : this.transactions) {
            if(!mempool.contains(transaction))
                return false;
        }

        return true;
    }
}
