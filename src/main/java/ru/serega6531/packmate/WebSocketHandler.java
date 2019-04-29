package ru.serega6531.packmate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.serega6531.packmate.service.PacketsSubscriptionService;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final PacketsSubscriptionService subscriptionService;

    @Autowired
    public WebSocketHandler(PacketsSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        subscriptionService.addSubscriber(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        subscriptionService.removeSubscriber(session);
    }
}
