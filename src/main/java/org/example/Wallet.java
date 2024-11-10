package org.example;
import java.security.*;
import java.security.spec.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.example.App.mempool;

public class Wallet {
    public PrivateKey privateKey;
    public PublicKey publicKey;
    public HashMap<String,TransactionOutput> UTXOs = new HashMap<>();

    public Wallet() {
        generateKeyPair();
    }

    public void generateKeyPair() {
        try {
            // "ECDSA": 타원곡선 암호화 방식을 이용한 디지털 서명 알고리즘
            // "BC": 암호화 제공자로 BouncyCastle를 지정 (BouncyCastle은 Java 암호화 확장 기능을 제공하는 오픈소스 라이브러리)
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");

            // SecureRandom은 암호학적으로 안전한 난수를 생성하는 클래스
            // SHA1PRNG는 SHA1 해시 함수를 기반으로 한 의사 난수 생성 알고리즘
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

            // ECGenParameterSpec은 타원곡선(EC) 파라미터를 지정하는 클래스
            // "prime192v1"은 NIST가 승인한 표준 타원곡선 중 하나, 192비트 길이의 소수 필드 위에 정의된 타원곡선
            ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("prime192v1");

            keyPairGenerator.initialize(ecGenParameterSpec, random);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 잔액 확인, UTXO 업데이트
    public float getBalance() {
        this.UTXOs = new HashMap<>();

        float total = 0;
        for (Map.Entry<String, TransactionOutput> entry : App.UTXOs.entrySet()) {
            TransactionOutput UTXO = entry.getValue();
            if (UTXO.isMine(publicKey)) {
                this.UTXOs.put(UTXO.id, UTXO);
                total += UTXO.value;
            }
        }
        return total;
    }


    /**
     * 트랜잭션 추가
     *
     * @param recipient 수신자
     * @param value 송신할 값
     * @return 생성된 트랜잭션 객체
     */
    public Transaction createTransaction(PublicKey recipient, float value) {
        if (getBalance() < value) {
            System.out.println("#Not Enough funds to send transaction. Transaction Discarded.");
            return null;
        }

        // 트랜잭션에 사용할 UTXO를 선택
        ArrayList<TransactionInput> inputs = new ArrayList<>();

        float total = 0;
        for (Map.Entry<String, TransactionOutput> entry : UTXOs.entrySet()) {
            TransactionOutput UTXO = entry.getValue();
            total += UTXO.value;
            inputs.add(new TransactionInput(UTXO.id));
            if (total > value) break;
        }

        // 트랜잭션 객체 생성
        Transaction transaction = new Transaction(publicKey, recipient, value, inputs);
        transaction.generateSignature(privateKey);

        // TODO: 트랜잭션 브로드캐스트
        if (transaction.validateTransaction()) {
            mempool.add(transaction);
            return transaction;
        }
        else {
            return null;
        }
    }
}
