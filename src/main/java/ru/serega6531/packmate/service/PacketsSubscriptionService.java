package ru.serega6531.packmate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import ru.serega6531.packmate.model.Stream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PacketsSubscriptionService {

    private List<WebSocketSession> subscribers = new ArrayList<>();

    private final ObjectMapper mapper;

    @Autowired
    public PacketsSubscriptionService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void addSubscriber(WebSocketSession session) {
        log.info("Подписан пользователь {}", session.getRemoteAddress().getHostName());
        subscribers.add(session);
    }

    public void removeSubscriber(WebSocketSession session) {
        log.info("Отписан пользователь {}", session.getRemoteAddress().getHostName());
        subscribers.remove(session);
    }

    public void broadcastNewStream(Stream stream) {
        subscribers.forEach(s -> {
            try {
                s.sendMessage(objectToTextMessage(stream));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private TextMessage objectToTextMessage(Object object) throws JsonProcessingException {
        return new TextMessage(mapper.writeValueAsString(object));
    }

}
