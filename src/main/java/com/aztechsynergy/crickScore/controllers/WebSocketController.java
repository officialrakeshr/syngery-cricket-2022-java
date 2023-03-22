package com.aztechsynergy.crickScore.controllers;

import com.aztechsynergy.crickScore.config.WebSocketConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/websocket")
@CrossOrigin(origins = "*", maxAge = 86400)
public class WebSocketController {
    /*************************WEB SOCKET APIS************************************/
    @Autowired
    private SimpMessagingTemplate template;

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public Object greeting() {
        return true;
    }


    @GetMapping("/reloadPage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> reloadPage() {
        System.out.println("Reloading page");
        this.template.convertAndSend("/topic/reloadPage", true);
        return ResponseEntity.ok(true);
    }
    @GetMapping("/pushMessage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> pushMessage(@RequestParam String msg) {
        this.template.convertAndSend("/topic/pushMessage", msg);
        return ResponseEntity.ok(WebSocketConfig.websocketSessionSet.size());
    }
}
