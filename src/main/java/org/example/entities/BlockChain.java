package org.example.entities;

import java.util.ArrayList;
import java.util.HashMap;

import static org.example.App.difficulty;
import static org.example.entities.Mempool.*;

public class BlockChain {
    public static ArrayList<Block> chain = new ArrayList<>();
    public static ArrayList<Block> branchCandidates = new ArrayList<>();
    public static final Object lockBlockChain = new Object();

    /**
     * 블록체인에 새로운 블록을 추가
     * <p>
     * previousHash가 마지막 블록 이전 블록일 시 분기 후보에 추가
     * <p>
     * 분기 후보에 블록이 존재하면, 새로운 블록의 이전 블록이 될 블록을 결정하고, 해당 블록으로 블록체인 ArrayList 재구성, 분기 후보 삭제
     * 분기에서 삭제된 블록들에 포함된 트랜잭션들은 mempool에 포함시키기
     * <p>
     * 분기 정보에 블록 없으면 그냥 현재 ArrayList에 추가
     *
     * @param block 추가할 블록
     * @return 블록이 성공적으로 추가되면 true, 그렇지 않으면 false
     */
    public static boolean addBlock(Block block) {
        synchronized (lockBlockChain) {
            // 블록 검증
            if (!block.validateBlock())
                return false;

            // genesis 블록이면 추가
            if (block.previousHash.equals("0")) {
                chain.add(block);
                purgeMempool(block);
                return true;
            }

            // 블록체인 분기 발생
            if (chain.size() > 2 && chain.get(chain.size()-2).hash.equals(block.previousHash)) {
                branchCandidates.add(block);
                purgeMempool(block);
                return true;
            }

            Block previousBlock = chain.get(chain.size()-1);
            if (branchCandidates.isEmpty()) {
                if (!previousBlock.hash.equals(block.previousHash))
                    return false;
            }
            else {
                if (!previousBlock.hash.equals(block.hash)) {
                    for (Block b : branchCandidates) {
                        previousBlock = b;
                        if (previousBlock.hash.equals(block.hash))
                            break;
                    }
                    if (!previousBlock.hash.equals(block.hash))
                        return false;

                    branchCandidates.add(chain.remove(chain.size()-1));
                    branchCandidates.remove(previousBlock);
                    chain.add(previousBlock);
                }
                for (Block b : branchCandidates) {
                    pool.addAll(b.transactions);
                }
                branchCandidates = new ArrayList<>();
            }
            chain.add(block);
            purgeMempool(block);
            return true;
        }
    }

    public static boolean isChainValid() {
        Block currentBlock;
        Block previousBlock;
        ArrayList<Block> chain = BlockChain.chain;
        String hashTarget = "0".repeat(difficulty);
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); //a temporary working list of unspent transactions at a given block state.
        for (Transaction genesisTransaction : chain.get(0).transactions) {
            tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
        }

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
            if(!currentBlock.hash.substring(0, difficulty).equals(hashTarget)) {
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
