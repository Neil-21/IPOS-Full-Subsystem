package main.service;

import com.google.gson.Gson;
import main.model.PUApplicationRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SAApplicationService {


    private static final String SA_APPLICATION_URL = "http://localhost:8080/api/pu-applications";

    private final HttpClient client;
    private final Gson gson;

    public SAApplicationService() {
        this.client = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public boolean submitCommercialApplication(PUApplicationRequest application) {
        try {
            String json = gson.toJson(application);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SA_APPLICATION_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("SA response code: " + response.statusCode());
            System.out.println("SA response body: " + response.body());

            return response.statusCode() == 201;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}