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
        BufferedReader bufferedReader = new BufferedReader(new FileReader(NODE_ADDRESSES_FILE));
        String line;
        while((line = bufferedReader.readLine()) != null) {
            publicKeyList.put(line, (PublicKey) sendGetRequest(line +PUBLICKEY_ENDPOINT).get());
        }

        // genesis 트랜잭션, 블록 생성
        bufferedReader = new BufferedReader(new FileReader(GENESIS_TRANSACTIONS_FILE));
        while((line = bufferedReader.readLine()) != null) {
            String socketAddress = line.split(" ")[0];
            float value = Float.parseFloat(line.split(" ")[1]);

            // 트랜잭션 생성
            Transaction genesisTransaction =  new Transaction(null, publicKeyList.get(socketAddress), value, null);
            genesisTransaction.transactionId = line; // 수동으로 지정
            TransactionOutput genesisTransactionOutput = new TransactionOutput(genesisTransaction.recipient, genesisTransaction.value, genesisTransaction.transactionId);
            genesisTransaction.outputs.add(genesisTransactionOutput);
            UTXOsMap.put(genesisTransactionOutput.id, genesisTransactionOutput);
            pool.add(genesisTransaction);
        }

        // 앞으로 broadcast 시 현재 호스트는 제외하기 위함
        publicKeyList.remove(InetAddress.getLocalHost().getHostAddress() + ":" + Integer.toString(currentServerPort));

        // genesis 블록 생성
        Block genesisBlock = new Block("0");
        if (genesisBlock.process()) {
            sendBroadcast(BLOCK_ENDPOINT, publicKeyList.keySet().toArray(new String[0]), genesisBlock);
            System.out.println("Genesis block "+ genesisBlock.hash +" has been mined successfully");
        }
        else {
            System.out.println("Fail to create genesis block");
        }

        Thread minorThread = new MinorThread();
        Thread userThread = new UserThread();

        minorThread.start();
        userThread.start();
    }

    public static void example() {
        Wallet walletA = new Wallet();
        Wallet walletB = new Wallet();
        Wallet coinbase = new Wallet();

        // Genesis 트랜잭션: 최초의 코인을 생성하는 트랜잭션
        // Genesis 트랜잭션은 Genesis 블록에 포함됨
        genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 100f, null);
        genesisTransaction.generateSignature(coinbase.privateKey);
        genesisTransaction.transactionId = "0"; // 수동으로 지정
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.recipient, genesisTransaction.value, genesisTransaction.transactionId));
        UTXOsMap.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
        pool.add(genesisTransaction);

        System.out.println("Creating and Mining Genesis block... ");
        Block genesis = new Block("0");
        genesis.transactions.addAll(pool);
        BlockChain.addBlock(genesis);


        System.out.println("\nBlock 1");
        System.out.println("A->B 40, A->B 20");

        Transaction transaction = walletA.createTransaction(walletB.publicKey, 40f);
        transaction.process();

        transaction = walletA.createTransaction(walletB.publicKey, 20f);
        transaction.process();

        Block block1 = new Block(genesis.hash);
        block1.process();

        System.out.println("\nBlock 2");
        System.out.println("A->B 10, B->A 5");

        transaction = walletA.createTransaction(walletB.publicKey, 10f);
        transaction.process();

        transaction = walletB.createTransaction(walletA.publicKey, 5f);
        transaction.process();

        Block block2 = new Block(block1.hash);
        block2.process();

        System.out.println("WalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        isChainValid();
    }

    public static Boolean isChainValid() {
        Block currentBlock;
        Block previousBlock;
        ArrayList<Block> chain = BlockChain.chain;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); //a temporary working list of unspent transactions at a given block state.
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        //loop through chain to check hashes:
        for(int i=1; i < chain.size(); i++) {

            currentBlock = chain.get(i);
            previousBlock = chain.get(i-1);
            //compare registered hash and calculated hash:
            if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
                System.out.println("#Current Hashes not equal");
                return false;
            }
            //compare previous hash and registered previous hash
            if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
                System.out.println("#Previous Hashes not equal");
                return false;
            }
            //check if hash is solved
            if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
                System.out.println("#This block hasn't been mined");
                return false;
            }

            //loop thru chains transactions:
            TransactionOutput tempOutput;
            for(int t=0; t <currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);

                if(!currentTransaction.verifySignature()) {
                    System.out.println("#Signature on Transaction(" + t + ") is Invalid");
                    return false;
                }
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#Inputs are not equal to outputs on Transaction(" + t + ")");
                    return false;
                }

                for(TransactionInput input: currentTransaction.inputs) {
                    tempOutput = tempUTXOs.get(input.transactionOutputId);

                    if(tempOutput == null) {
                        System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
                        return false;
                    }

                    if(input.UTXO.value != tempOutput.value) {
                        System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
                        return false;
                    }

                    tempUTXOs.remove(input.transactionOutputId);
                }

                for(TransactionOutput output: currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }

                if( currentTransaction.outputs.get(0).recipient != currentTransaction.recipient) {
                    System.out.println("#Transaction(" + t + ") output reciepient is not who it should be");
                    return false;
                }
                if( currentTransaction.outputs.get(1).recipient != currentTransaction.sender) {
                    System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
                    return false;
                }

            }

        }
        System.out.println("Blockchain is valid");
        return true;
    }
}
