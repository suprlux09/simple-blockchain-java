package org.example;

import java.util.ArrayList;
import java.util.Date;

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

    // 블록에 트랜잭션 추가
    public boolean addTransactioninBlock(Transaction transaction) {
        if (transaction == null) return false;

        // 제네시스 블록(블록체인의 첫번째 블록)이 아닌 경우
        // 트랜잭션 처리 및 유효성 검사
        if (previousHash != "0") {
            if (!transaction.processTransaction()) {
                System.out.println("Transaction failed to process. Discarded.");
                return false;
            }
        }
        transactions.add(transaction);
        System.out.println("Transaction Successfully added to Block");
        return true;
    }
}
