package com.backend.ChatGPT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Iterator;

@RestController
public class ChatGPTController {
    // This annotation injects the value of the "api.key" property from the application.properties
    @Value("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") private String openaiApiKey;
    // Create a new HTTP client instance
    private HttpClient client = HttpClient.newHttpClient() ;
    // create a constant URI for the OpenAI completions endpoint
    private static final URI CHATGPT_URI = URI.create("https://api.openai.com/v1/completions");

    // method handles POST requests to the "/chatGPT" endpoint

    @Operation(summary  = "Chat with ChatGPT", description  = "Send Request & Returns a answer from ChatGPT ")
    @PostMapping("/chatGPT")
    public ResponseEntity<String> chatGPT(@RequestBody DataClient question) throws IOException, InterruptedException {

        /* Create DataRequest object based on the incoming question */
        DataRequest dataRequest = new DataRequest();
        dataRequest.setModel("text-davinci-003");
        dataRequest.setPrompt(question.getQuestion());
        dataRequest.setMax_tokens(4000);
        dataRequest.setTemperature(1.0);

        // Convert the DataRequest object to a JSON string
        ObjectMapper mapper1 = new ObjectMapper();
        String dataRequestJson = mapper1.writeValueAsString(dataRequest);

        /*create a chatGPT request using the provided question (dataRequest) with the JSON string as the request body */
        var request = HttpRequest.newBuilder()
                .uri(CHATGPT_URI)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(dataRequestJson))
                .build();

        /* Send the HTTP request and retrieve the response body as a string */
        var responseBody = client.send(request, HttpResponse.BodyHandlers.ofString()).body();

        /* Extract the answer from the response form chat gpt */
        ObjectMapper mapper2 = new ObjectMapper();
        JsonNode jsonNode = mapper2.readTree(responseBody);
        JsonNode choicesNode = jsonNode.get("choices");
        String dataResponse = null;
        if (choicesNode.isArray()) {
            Iterator<JsonNode> choicesIter = choicesNode.elements();
            while (choicesIter.hasNext()) {
                JsonNode personNode = choicesIter.next();
                dataResponse = personNode.get("text").asText();
                System.out.println("Answer=  " + dataResponse);
            }
        }

        /* Append the question and answer to a csv file */

        String filePath = "./data_csv/Question-Answer.csv";
        File file = new File(filePath);

        BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
        // Add the header if not exist
        if (!file.exists() || file.length() == 0) {
            String header = "Question,Answer\n";
            writer.write(header);
        }
        // Add the question and the answer as row
        String row = question.getQuestion() + ";" + dataResponse.replaceAll("\r", " ").replaceAll("\n", " ") + "\n";
        writer.write(row);
        writer.close();

        /* respond to the user with the answer provided by chatgpt */
        return ResponseEntity.ok(dataResponse);

    }

}
