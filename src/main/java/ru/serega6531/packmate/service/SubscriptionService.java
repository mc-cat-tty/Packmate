package ru.serega6531.packmate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import ru.serega6531.packmate.model.pojo.SubscriptionMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class SubscriptionService {

    private final List<WebSocketSession> subscribers = Collections.synchronizedList(new ArrayList<>());

    private final ObjectMapper mapper;

    @Autowired
    public SubscriptionService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void addSubscriber(WebSocketSession session) {
        subscribers.add(session);
        log.info("User subscribed: {}", Objects.requireNonNull(session.getRemoteAddress()).getHostName());
    }

    public void removeSubscriber(WebSocketSession session) {
        subscribers.remove(session);
        log.info("User unsubscribed {}", Objects.requireNonNull(session.getRemoteAddress()).getHostName());
    }

    void broadcast(SubscriptionMessage message) {
        subscribers.forEach(s -> {
            try {
                s.sendMessage(objectToTextMessage(message));
            } catch (IOException | SockJsTransportFailureException e) {
                log.warn("WS", e);
            }
        });
    }

    private TextMessage objectToTextMessage(SubscriptionMessage object) throws JsonProcessingException {
        return new TextMessage(mapper.writeValueAsString(object));
    }

}
