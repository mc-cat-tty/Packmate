package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.model.FoundPattern;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.model.enums.PatternDirectionType;
import ru.serega6531.packmate.model.enums.PatternSearchType;
import ru.serega6531.packmate.model.enums.SubscriptionMessageType;
import ru.serega6531.packmate.model.pojo.SubscriptionMessage;
import ru.serega6531.packmate.repository.PatternRepository;

import java.util.*;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PatternService {

    private final PatternRepository repository;
    private final SubscriptionService subscriptionService;

    private final Map<Integer, Pattern> patterns = new HashMap<>();

    @Autowired
    public PatternService(PatternRepository repository,
                          SubscriptionService subscriptionService) {
        this.repository = repository;
        this.subscriptionService = subscriptionService;

        repository.findAll().forEach(p -> patterns.put(p.getId(), p));
        log.info("Loaded {} patterns", patterns.size());
    }

    public Pattern find(int id) {
        return patterns.get(id);
    }

    public Collection<Pattern> findAll() {
        return patterns.values();
    }

    public Set<FoundPattern> findMatches(byte[] bytes, boolean incoming) {
        final List<Pattern> list = patterns.values().stream()
                .filter(Pattern::isEnabled)
                .filter(p -> p.getDirectionType() == (incoming ? PatternDirectionType.INPUT : PatternDirectionType.OUTPUT)
                        || p.getDirectionType() == PatternDirectionType.BOTH)
                .collect(Collectors.toList());
        return new PatternMatcher(bytes, list).findMatches();
    }

    public void enable(int id, boolean enabled) {
        final Pattern pattern = find(id);
        if (pattern != null) {
            pattern.setEnabled(enabled);
            final Pattern saved = repository.save(pattern);
            patterns.put(id, saved);

            if (enabled) {
                log.info("Включен паттерн {} со значением {}", pattern.getName(), pattern.getValue());
                subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.ENABLE_PATTERN, id));
            } else {
                log.info("Выключен паттерн {} со значением {}", pattern.getName(), pattern.getValue());
                subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.DISABLE_PATTERN, id));
            }
        }
    }

    public Pattern save(Pattern pattern) {
        if (pattern.getSearchType() == PatternSearchType.REGEX) {
            try {
                PatternMatcher.compilePattern(pattern);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        final Pattern saved = repository.save(pattern);
        patterns.put(saved.getId(), saved);
        log.info("Added new pattern {} with value {}", pattern.getName(), pattern.getValue());
        subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.SAVE_PATTERN, saved));
        return saved;
    }

}
