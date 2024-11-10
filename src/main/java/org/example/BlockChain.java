package org.example;

import java.util.ArrayList;

import static org.example.App.mempool;

public class BlockChain {
    ArrayList<Block> chain = new ArrayList<>();
    ArrayList<Block> branchCandidates = new ArrayList<>();

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
    public boolean addBlock(Block block) {
        // genesis 블록 추가
        if (block.previousHash.equals("0")) {
            chain.add(block);
            for (Transaction t : block.transactions) {
                mempool.remove(t);
            }
            return true;
        }

        // 블록 검증
        if (!block.validateBlock())
            return false;

        // 블록체인 분기 발생
        if (chain.size() > 2 && chain.get(chain.size()-2).hash.equals(block.previousHash)) {
            branchCandidates.add(block);
            for (Transaction t : block.transactions) {
                mempool.remove(t);
            }
            return true;
        }

        if (branchCandidates.isEmpty()) {
            Block previousBlock = chain.get(chain.size()-1);
            if (!previousBlock.hash.equals(block.previousHash))
                return false;
            chain.add(block);
        }
        else {
            Block previousBlock = chain.get(chain.size()-1);
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
                mempool.addAll(b.transactions);
            }
            branchCandidates = new ArrayList<>();

            chain.add(block);
        }

        for (Transaction t : block.transactions) {
            mempool.remove(t);
        }
        return true;
    }
}
