package org.example.utilities;

import java.io.*;
import java.net.InetAddress;
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

    public static <T extends Serializable> CompletableFuture<T> sendGetRequest(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .build();

        return returnCompletableFuture(request);
    }

    public static void sendPostRequest(String url, byte[] requestPayload) {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestPayload))
                .uri(URI.create(url))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static <T extends Serializable> CompletableFuture<T> returnCompletableFuture(HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(HttpResponse::body)
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

    public static <T extends Serializable> void sendBroadcast(String path, String[] socketAddresses, T obj) throws IOException {
        byte[] data = serializeObjectToByteArray(obj);
        // TODO: 자기 자신에게는 브로드캐스트 요청 보내지 않도록 하기
        for (String socketAddress : socketAddresses) {
            sendPostRequest( socketAddress+path, data);
        }
    }
}
