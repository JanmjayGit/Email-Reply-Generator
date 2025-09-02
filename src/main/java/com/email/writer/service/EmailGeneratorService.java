package com.email.writer.service;

import com.email.writer.model.EmailRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value( "${gemini.api.url}")
    private String geminiApiUrl;
    @Value( "${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) { // this will be injected during the runtime in Spring
        this.webClient = webClientBuilder.build();
    }


    public String generateEmailReply(EmailRequest emailRequest) {
        try {
            String prompt = buildPrompt(emailRequest);
            System.out.println("Prompt: " + prompt); // debug log

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", prompt)
                                    )
                            )
                    )
            );


            String response = webClient.post()
                    .uri(geminiApiUrl + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("Gemini API response: " + response); // debug log

            return extratResponseContent(response);

        } catch (Exception e) {
            e.printStackTrace(); // show full error in console
            return "Server error: " + e.getMessage();
        }
    }


    private String extratResponseContent(String response) {
        try{

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response); // json return the data in tree like structure
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            return "Error processing request:" + e.getMessage();
        }

    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email reply for the following email content. Please don't generate a subject line");

        if(emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Use a").append(emailRequest.getTone()).append(" tone.");
        }

        prompt.append("\nOriginal email:\n").append(emailRequest.getEmailContent()); // taking care of track of email content
        return prompt.toString();
    }
}
