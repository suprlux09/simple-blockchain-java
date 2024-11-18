package org.example.entities;

import java.security.PublicKey;

import static org.example.utilities.Hash.applySHA256;
import static org.example.utilities.DigitalSignature.getStringFromKey;

public class TransactionOutput {
    public String id;
    public PublicKey recipient;
    public float value;
    public String parentTransactionId; // output이 생성된 트랜잭션의 id

    public TransactionOutput(PublicKey recipient, float value, String parentTransactionId) {
        this.recipient = recipient;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
        this.id = applySHA256(getStringFromKey(recipient)+Float.toString(value)+parentTransactionId);
    }

    public boolean isMine(PublicKey publicKey) {
        return (publicKey == recipient);
    }
}
