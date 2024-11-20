package org.example.utilities;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import static org.example.utilities.Serialization.*;

public class WebRequest {
    public static final String PUBLICKEY_ENDPOINT = "/publickey";
    public static final String BLOCK_ENDPOINT = "/block";
    public static final String TRANSACTION_ENDPOINT = "/transaction";

    public static HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    public static <T extends Serializable> CompletableFuture<T> sendGetRequest(String address) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("http://"+address))
                .build();

        return attemptSendAsync(request).thenApply(HttpResponse::body)
                .thenApply(responseBody -> {
                    try {
                        @SuppressWarnings("unchecked")
                        T obj = (T) deserializeObjectFromByteArray(responseBody);
                        return obj;
                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public static void sendPostRequest(String address, byte[] requestPayload) {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestPayload))
                .uri(URI.create("http://"+address))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static CompletableFuture<HttpResponse<byte[]>> attemptSendAsync(HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .exceptionallyCompose(ex -> {
                    try {
                        System.out.println("Fail to connect, retry after 5 seconds..");
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return attemptSendAsync(request);
                });
    }

    public static <T extends Serializable> void sendBroadcast(String path, String[] socketAddresses, T obj) throws IOException {
        byte[] data = serializeObjectToByteArray(obj);
        for (String socketAddress : socketAddresses) {
            sendPostRequest( socketAddress+path, data);
        }
    }
}
