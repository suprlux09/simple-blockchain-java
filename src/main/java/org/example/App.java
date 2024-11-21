package org.example;

import java.io.*;
import java.net.InetAddress;
import java.security.PublicKey;
import java.security.Security;
import java.util.*;
import java.util.concurrent.ExecutionException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.example.entities.*;
import org.example.threads.MinorThread;
import org.example.threads.UserThread;
import org.example.threads.WebReceiverThread;

import static org.example.entities.Mempool.pool;
import static org.example.entities.UTXOs.*;
import static org.example.utilities.WebRequest.*;

public class App  {
    private static final String NODE_ADDRESSES_FILE = "NodeAddresses";
    private static final String GENESIS_TRANSACTIONS_FILE = "GenesisTransactions";

    public static int difficulty = 5;
    public static int minimumTransactionsPerBlock = 1;
    public static float minimumTransaction = 0.1f;

    public static Transaction genesisTransaction;
    public static Wallet wallet;
    public static HashMap<String, PublicKey> publicKeyList = new HashMap<>();

    static {
        // 암호화 제공자로 BouncyCastle를 지정
        Security.addProvider(new BouncyCastleProvider());
        wallet = new Wallet();
    }

    public static void main( String[] args ) throws IOException, ExecutionException, InterruptedException {
        int currentServerPort = 8080;
        if (args.length == 1) {
            currentServerPort = Integer.parseInt(args[0]);
        }

        Thread webReceiverThread = new WebReceiverThread(currentServerPort);
        webReceiverThread.start();

        // publickey 리스트 받기
        publicKeyList.put(InetAddress.getLocalHost().getHostAddress() + ":" + Integer.toString(currentServerPort), wallet.publicKey);  // 현재 호스트
        BufferedReader bufferedReader = new BufferedReader(new FileReader(NODE_ADDRESSES_FILE));
        String line;
        while((line = bufferedReader.readLine()) != null) {
            publicKeyList.putIfAbsent(line, (PublicKey) sendGetRequest(line +PUBLICKEY_ENDPOINT).get());
        }

        // genesis 트랜잭션, 블록 생성
        bufferedReader = new BufferedReader(new FileReader(GENESIS_TRANSACTIONS_FILE));
        while((line = bufferedReader.readLine()) != null) {
            String address = line.split(" ")[0];
            float value = Float.parseFloat(line.split(" ")[1]);

            // 트랜잭션 생성
            Transaction genesisTransaction =  new Transaction(null, publicKeyList.get(address), value, null);
            genesisTransaction.transactionId = line; // 수동으로 지정
            TransactionOutput genesisTransactionOutput = new TransactionOutput(genesisTransaction.recipient, genesisTransaction.value, genesisTransaction.transactionId);
            genesisTransaction.outputs.add(genesisTransactionOutput);
            UTXOsMap.put(genesisTransactionOutput.id, genesisTransactionOutput);
            pool.add(genesisTransaction);
        }

        // 앞으로 broadcast로 전송하는 과정에서 현재 호스트는 제외하기 위함
        publicKeyList.remove(InetAddress.getLocalHost().getHostAddress() + ":" + Integer.toString(currentServerPort));

        // genesis 블록 생성
        Block genesisBlock = new Block("0");
        if (genesisBlock.process()) {
            sendBroadcast(BLOCK_ENDPOINT, publicKeyList.keySet().toArray(new String[0]), genesisBlock);
            System.out.println("Genesis block "+ genesisBlock +" has been mined successfully");
        }
        else {
            System.out.println("Fail to create genesis block");
        }

        Thread minorThread = new MinorThread();
        Thread userThread = new UserThread();

        minorThread.start();
        userThread.start();
    }
}
