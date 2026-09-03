package ec.edu.uteq.presustentaciones.controllers;

import ec.edu.uteq.presustentaciones.dto.ChatRequest;
import ec.edu.uteq.presustentaciones.dto.ChatResponse;
import ec.edu.uteq.presustentaciones.services.ChatbotService;
import lombok.RequiredArgsConstructor;
import ec.edu.uteq.presustentaciones.dto.ResponseWrapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    public ResponseEntity<ResponseWrapper<ChatResponse>> askChatbot(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(ResponseWrapper.success(chatbotService.processMessage(request), "Consulta procesada correctamente"));
    }
}
