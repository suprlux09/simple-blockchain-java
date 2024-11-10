package org.example;

import java.lang.reflect.Array;
import java.security.Security;
import java.util.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.util.ArrayList;

public class App  {
    public static HashMap<String, TransactionOutput> UTXOs = new HashMap<>();
    public static ArrayList<Transaction> mempool = new ArrayList<>();
    public static int difficulty = 2;
    public static float minimumTransaction = 0.1f;

    public static Transaction genesisTransaction;

    public static void main( String[] args ) {
        // 암호화 제공자로 BouncyCastle를 지정
        Security.addProvider(new BouncyCastleProvider());


        // 새로운 트랜잭션을 입력받는 스레드
        // 블록 생성하는 스레드
        // 네트워크로부터 블록, 트랜잭션 정보 수신받는 스레드
        BlockChain blockChain = new BlockChain();

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
        mempool.add(genesisTransaction);

        System.out.println("Creating and Mining Genesis block... ");
        Block genesis = new Block("0");
        genesis.transactions.addAll(mempool);
        blockChain.addBlock(genesis);

        System.out.println("\nBlock 1");
        System.out.println("A->B 40, A->B 20");

        // 트랜잭션 생성
        walletA.createTransaction(walletB.publicKey, 40f);
        walletA.createTransaction(walletB.publicKey, 20f);

        // 현재 생성된 트랜잭션 가지고 블록 생성
        Block block1 = new Block(genesis.hash);
        block1.transactions.addAll(mempool);
        block1.mineBlock(2);
        blockChain.addBlock(block1);

        // Block 2
        System.out.println("\nBlock 2");
        System.out.println("A->B 10, B->A 5");

        walletA.createTransaction(walletB.publicKey, 10f);
        walletB.createTransaction(walletA.publicKey, 5f);
        Block block2 = new Block(block1.hash);
        block2.transactions.addAll(mempool);
        block2.mineBlock(2);
        blockChain.addBlock(block2);

        System.out.println("WalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());


        isChainValid(blockChain);
    }


    public static Boolean isChainValid(BlockChain blockChain) {
        Block currentBlock;
        Block previousBlock;
        ArrayList<Block> chain = blockChain.chain;
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
                    System.out.println("#Inputs are note equal to outputs on Transaction(" + t + ")");
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
