package org.example.threads;

import com.google.gson.GsonBuilder;
import org.example.entities.Transaction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.example.App.publicKeyList;
import static org.example.App.wallet;
import static org.example.entities.BlockChain.*;
import static org.example.utilities.WebRequest.*;

public class UserThread extends Thread{
    @Override
    public void run() {
        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));) {
            System.out.println("\n[send] coin\nview my [balance]\nview block[chain]\n");
            while (true) {
                String input = bufferedReader.readLine();

                if (input.equals("send")) {
                    System.out.print("[to] [value]: ");
                    input = bufferedReader.readLine();
                    String address = input.split(" ")[0];
                    float value = Float.parseFloat(input.split(" ")[1]);
                    if (!publicKeyList.containsKey(address)) {
                        System.out.println("Address not exists");
                    }
                    else {
                        Transaction transaction = wallet.createTransaction(publicKeyList.get(address), value);
                        if (transaction.process()) {
                            sendBroadcast(TRANSACTION_ENDPOINT, publicKeyList.keySet().toArray(new String[0]), transaction);
                            System.out.println("Transaction created");
                        }
                        else {
                            System.out.println("Transaction creation FAILED");
                        }
                    }
                }
                else if (input.equals("balance")) {
                    System.out.println(wallet.getBalance());
                }
                else if (input.equals("chain")) {
                    System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(chain));
                    isChainValid();
                }
                else if (!input.isEmpty()) {
                    System.out.println("Wrong command");
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
