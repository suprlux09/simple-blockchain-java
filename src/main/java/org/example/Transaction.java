package org.example;

import java.security.*;
import java.util.ArrayList;

import static org.example.App.minimumTransaction;

public class Transaction {

    public String transactionId;
    public PublicKey sender;
    public PublicKey recipient;
    public float value;
    public byte[] signature;  // 디지털 서명

    public ArrayList<TransactionInput> inputs = new ArrayList<>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<>();

    private static int sequence = 0;  // 지금까지 생성된 트랜잭션의 개수

    public Transaction(PublicKey from, PublicKey to, float value,  ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
        this.transactionId = calulateHash();
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
    private String calulateHash() {
        sequence++; //increase the sequence to avoid 2 identical transactions having the same hash
        return Utility.applySHA256(
                Utility.getStringFromKey(sender) +
                    Utility.getStringFromKey(recipient) +
                    Float.toString(value) + sequence
        );
    }

    // 디지털 서명 생성
    public void generateSignature(PrivateKey privateKey) {
        String data = Utility.getStringFromKey(sender) + Utility.getStringFromKey(recipient)  + Float.toString(value);
        signature = Utility.applyECDSASig(privateKey, data);
    }

    // 디지털 서명 검증 (송신자의 publicKey)
    public boolean verifySignature() {
        String data = Utility.getStringFromKey(sender) + Utility.getStringFromKey(recipient)  + Float.toString(value);
        return Utility.verifyECDSASig(sender, data, signature);
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
            input.UTXO = App.UTXOs.get(input.transactionOutputId);
            if (input.UTXO == null)
                return false;
        }

        if (getInputsValue() - value < minimumTransaction) {
            System.out.println("#Transaction Inputs to small: " + getInputsValue());
            return false;
        }

        // 새로운 UTXO 생성(Output)
        outputs.add(new TransactionOutput(this.recipient, value, transactionId));
        outputs.add(new TransactionOutput(this.sender, getInputsValue() - value, transactionId));

        // 사용된 Input을 UTXO로부터 제거
        for (TransactionInput input : inputs) {
            App.UTXOs.remove(input.UTXO.id);
        }

        // 생성된 Output을 UTXO에 추가
        for (TransactionOutput output : outputs) {
            App.UTXOs.put(output.id, output);
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
