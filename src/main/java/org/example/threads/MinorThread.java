package org.example.threads;

public class MinorThread extends Thread {
    @Override
    public void run() {
        // mempool이 비어 있으면 블록

        // mempool이 비어 있지 않으면 현재 mempool에 있는 트랜잭션 리스트를 가져다가 블록 채굴 시작
        // 블록을 채굴하면서도 mempool에 트랜잭션이 추가될 수 있어야 함
        // mempool에서 트랜잭션을 복사하는 과정에서만 락이 필요

        // 채굴 완료되면 블록체인에 추가 후 추가 성공 시 브로드캐스트 전송

    }
}
