package org.example.entities;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;

import static org.example.App.*;
import static org.example.entities.BlockChain.addBlock;
import static org.example.entities.Mempool.pool;
import static org.example.utilities.Hash.*;

public class Block implements Serializable {

    public String hash;
    public String previousHash;
    public PublicKey minor;
    private String merkleRoot;
    public ArrayList<Transaction> transactions = new ArrayList<>();
    private long timeStamp;
    private int nonce;

    public Block(String previousHash) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.minor = wallet.publicKey;
        this.hash = calculateHash();
    }

    public String calculateHash() {
        return applySHA256(previousHash + merkleRoot + minor +Long.toString(timeStamp) + Integer.toString(nonce));
    }

    /**
     * 생성 및 수신한 블록의 처리
     * <p>
     * 생성된 블록일 경우 채굴하고 블록체인에 추가
     * <p>
     * 채굴된 블록을 수신한 경우, 검증하고 블록체인에 추가
     *
     * @return 블록체인 추가에 성공했을 시 true
     */
    public boolean process() {
        if (minor.equals(wallet.publicKey)) {
            synchronized (pool) {
                while (pool.size() < minimumTransactionsPerBlock) {
                    try {
                        pool.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                this.transactions.addAll(pool);
            }
            this.mineBlock(difficulty);
            return addBlock(this);
        }
        else {
            return this.validateBlock() && addBlock(this);
        }
    }

    public void mineBlock(int difficulty) {
        this.merkleRoot = getMerkleRoot(transactions);
        String target = "0".repeat(difficulty);
        while(!hash.substring(0, difficulty).equals(target)) {
            nonce += 1;
            hash = calculateHash();
        }
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
            if(!pool.contains(transaction))
                return false;
        }

        return true;
    }
}
