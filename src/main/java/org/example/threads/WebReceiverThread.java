package org.example.threads;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.example.entities.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import static org.example.utilities.WebRequest.*;
import static org.example.utilities.Serialization.*;
import static org.example.App.wallet;

public class WebReceiverThread {
    private final int port;
    private HttpServer server;

    public WebReceiverThread(int port) throws IOException {
        this.port = port;
        run();
    }

    public void run() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        HttpContext publicKeyContext = server.createContext(PUBLICKEY_ENDPOINT);
        HttpContext blockContext = server.createContext(BLOCK_ENDPOINT);
        HttpContext transactionContext = server.createContext(TRANSACTION_ENDPOINT);

        publicKeyContext.setHandler(this::handlePublicKeyRequest);
        blockContext.setHandler(this::handleAcceptBlock);
        transactionContext.setHandler(this::handleAcceptTransaction);

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    // transaction 수신
    private void handleAcceptTransaction(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
            exchange.close();
            return;
        }

        try (InputStream in = exchange.getRequestBody()) {
            Transaction transaction = (Transaction) deserializeObjectFromByteArray(in.readAllBytes());
            transaction.process();
            exchange.sendResponseHeaders(204, -1);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        exchange.close();
    }

    // block 수신
    private void handleAcceptBlock(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
            exchange.close();
            return;
        }

        try (InputStream in = exchange.getRequestBody()) {
            Block block = (Block) deserializeObjectFromByteArray(in.readAllBytes());
            block.process();
            exchange.sendResponseHeaders(204, -1);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        exchange.close();
    }

    // publickey 보내달라는 요청 수신
    private void handlePublicKeyRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.close();
            return;
        }

        byte[] data = serializeObjectToByteArray(wallet.publicKey);

        try (OutputStream out = exchange.getResponseBody()) {
            exchange.sendResponseHeaders(200, data.length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(data);
            outputStream.flush();
            outputStream.close();
        }
    }
}
