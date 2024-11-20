package org.example.entities;

import java.io.Serializable;

public class TransactionInput implements Serializable {
    public String transactionOutputId; // 이 id값으로 아래 UTXO가 참조할 객체를 가져오게 됨
    public TransactionOutput UTXO;

    public TransactionInput(String transactionOutputId) {
        this.transactionOutputId = transactionOutputId;
    }
}
