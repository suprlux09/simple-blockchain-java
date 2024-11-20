package org.example.entities;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UTXOs {
    public static final Map<String, TransactionOutput> UTXOsMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * 채굴된 블록이나 수신받은 트랜잭션 중 검증된 트랜잭션에 대해
     * 트랜잭션의 output(트랜잭션 수행 후 생성된 UTXO)를 업데이트하고, 이들을 UTXO 리스트에 추가
     * <p>
     * input(트랜잭션을 수행하는 데 사용된 UTXO)을 UTXO 리스트에서 제거
     *
     * @param transaction 생성된 트랜잭션 또는 수신받은 트랜잭션 중 검증된 트랜잭션
     */
    public static void updateUTXOs(Transaction transaction) {
        transaction.outputs.add(new TransactionOutput(transaction.recipient, transaction.value, transaction.transactionId));
        transaction.outputs.add(new TransactionOutput(transaction.sender, transaction.getInputsValue() - transaction.value, transaction.transactionId));

        for (TransactionInput input : transaction.inputs) {
            UTXOsMap.remove(input.UTXO.id);
        }

        for (TransactionOutput output : transaction.outputs) {
            UTXOsMap.put(output.id, output);
        }
    }
}
