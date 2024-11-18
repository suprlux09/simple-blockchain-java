package org.example;

import java.security.Security;
import java.util.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.example.entities.*;
import static org.example.entities.Mempool.pool;
import static org.example.entities.UTXOs.*;

import java.util.ArrayList;

public class App  {
    public static int difficulty = 5;
    public static float minimumTransaction = 0.1f;

    public static Transaction genesisTransaction;

    public static void main( String[] args ) {

        // 암호화 제공자로 BouncyCastle를 지정
        Security.addProvider(new BouncyCastleProvider());

        // publickey 리스트 받기

        // genesis 트랜잭션 생성, 전송

        // genesis 블록 생성, 전송

        // 실행
        example();

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
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
        pool.add(genesisTransaction);

        System.out.println("Creating and Mining Genesis block... ");
        Block genesis = new Block("0");
        genesis.transactions.addAll(pool);
        BlockChain.addBlock(genesis);


        System.out.println("\nBlock 1");
        System.out.println("A->B 40, A->B 20");

        Transaction transaction = walletA.createTransaction(walletB.publicKey, 40f);
        if (transaction.validateTransaction()) {
            updateUTXOs(transaction);
            pool.add(transaction);
        }
        transaction = walletA.createTransaction(walletB.publicKey, 20f);
        if (transaction.validateTransaction()) {
            updateUTXOs(transaction);
            pool.add(transaction);
        }

        Block block1 = new Block(genesis.hash);
        block1.transactions.addAll(pool);
        block1.mineBlock(2);
        BlockChain.addBlock(block1);


        System.out.println("\nBlock 2");
        System.out.println("A->B 10, B->A 5");

        transaction = walletA.createTransaction(walletB.publicKey, 10f);
        if (transaction.validateTransaction()) {
            updateUTXOs(transaction);
            pool.add(transaction);
        }
        transaction = walletB.createTransaction(walletA.publicKey, 5f);
        if (transaction.validateTransaction()) {
            updateUTXOs(transaction);
            pool.add(transaction);
        }

        Block block2 = new Block(block1.hash);
        block2.transactions.addAll(pool);
        block2.mineBlock(2);
        BlockChain.addBlock(block2);

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
