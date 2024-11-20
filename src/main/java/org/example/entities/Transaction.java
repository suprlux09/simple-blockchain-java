package org.example.entities;

import java.io.Serializable;
import java.security.*;
import java.util.ArrayList;

import static org.example.App.minimumTransaction;
import static org.example.entities.Mempool.pool;
import static org.example.entities.UTXOs.*;
import static org.example.utilities.Hash.*;
import static org.example.utilities.DigitalSignature.*;

public class Transaction implements Serializable {

    public String transactionId;
    public PublicKey sender;
    public PublicKey recipient;
    public float value;
    public byte[] signature;  // 디지털 서명

    public ArrayList<TransactionInput> inputs = new ArrayList<>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<>();

    private static int sequence = 0;  // 지금까지 생성된 트랜잭션의 개수

    public Transaction(PublicKey from, PublicKey to, float value, ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
        this.transactionId = calculateHash();
    }

    @Override
    public boolean equals(Object obj) {
        Transaction t = (Transaction) obj;
        if (!this.transactionId.equals(t.transactionId))
            return false;
        if (!this.sender.equals(t.sender))
            return false;
        if (!this.recipient.equals(t.recipient))
            return false;
        if (this.value != t.value)
            return false;
        if (!this.signature.equals(t.signature))
            return false;

        return true;
    }

    // 트랜잭션의 해시값, transactionId로도 사용됨
    private String calculateHash() {
        sequence++; //increase the sequence to avoid 2 identical transactions having the same hash
        return applySHA256(
                getStringFromKey(sender) +
                    getStringFromKey(recipient) +
                    Float.toString(value) + sequence
        );
    }

    // 디지털 서명 생성
    public void generateSignature(PrivateKey privateKey) {
        String data = getStringFromKey(sender) + getStringFromKey(recipient)  + Float.toString(value);
        signature = applyECDSASig(privateKey, data);
    }

    // 디지털 서명 검증 (송신자의 publicKey)
    public boolean verifySignature() {
        String data = getStringFromKey(sender) + getStringFromKey(recipient)  + Float.toString(value);
        return verifyECDSASig(sender, data, signature);
    }

    /**
     * 생성 및 수신한 트랜잭션의 처리
     * <p>
     * 트랜잭션을 검증하고, 검증이 성공하면 UTXO 업데이트 및 mempool에 트랜잭션 추가
     *
     * @return 검증되지 않았거나 이미 처리된 트랜잭션일 경우 false
     */
    public boolean process() {
        if (this.validateTransaction() && !pool.contains(this)) {
            updateUTXOs(this);
            synchronized (pool) {
                pool.add(this);
                pool.notify();
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * 트랜잭션 검증
     * <p>
     * 디지털 서명과 inputs에 올바른 UTXO 정보가 포함되었는지를 검증
     *
     * @return 유효하면 true, 유효하지 않으면 false
     */
    public boolean validateTransaction() {
        // 디지털 서명 검증
        if (!verifySignature()) {
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }

        // Input에 저장된 id로부터 객체 참조하도록 하기
        for (TransactionInput input : inputs) {
            input.UTXO = UTXOsMap.get(input.transactionOutputId);
            if (input.UTXO == null)
                return false;
        }

        if (getInputsValue() - value < minimumTransaction) {
            System.out.println("#Transaction Inputs to small: " + getInputsValue());
            return false;
        }

        return true;
    }

    public float getInputsValue() {
        float total = 0;
        for (TransactionInput input : inputs) {
            if (input.UTXO == null) continue; //if Transaction can't be found skip it
            total += input.UTXO.value;
        }
        return total;
    }

    public float getOutputsValue() {
        float total = 0;
        for(TransactionOutput output : outputs) {
            total += output.value;
        }
        return total;
    }
}
