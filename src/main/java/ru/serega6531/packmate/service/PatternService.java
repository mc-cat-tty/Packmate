package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.serega6531.packmate.model.FoundPattern;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.model.Stream;
import ru.serega6531.packmate.model.enums.PatternDirectionType;
import ru.serega6531.packmate.model.enums.PatternSearchType;
import ru.serega6531.packmate.model.enums.SubscriptionMessageType;
import ru.serega6531.packmate.model.pojo.SubscriptionMessage;
import ru.serega6531.packmate.repository.PatternRepository;
import ru.serega6531.packmate.utils.Bytes;

import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PatternService {

    private final PatternRepository repository;
    private final StreamService streamService;
    private final StreamSubscriptionService subscriptionService;

    private final Map<Integer, Pattern> patterns = new HashMap<>();
    private final Map<String, java.util.regex.Pattern> compiledPatterns = new HashMap<>();

    @Autowired
    public PatternService(PatternRepository repository,
                          @Lazy StreamService streamService,
                          StreamSubscriptionService subscriptionService) {
        this.repository = repository;
        this.streamService = streamService;
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
        return patterns.values().stream()
                .filter(p -> p.getDirectionType() == (incoming ? PatternDirectionType.INPUT : PatternDirectionType.OUTPUT)
                        || p.getDirectionType() == PatternDirectionType.BOTH)
                .map(pattern -> match(pattern, bytes))
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }

    private List<FoundPattern> match(Pattern pattern, byte[] bytes) {
        List<FoundPattern> found = new ArrayList<>();

        if (pattern.getSearchType() == PatternSearchType.REGEX) {
            String content = new String(bytes);
            final java.util.regex.Pattern regex = compilePattern(pattern);
            final Matcher matcher = regex.matcher(content);
            int startPos = 0;

            while (matcher.find(startPos)) {
                found.add(FoundPattern.builder()
                        .patternId(pattern.getId())
                        .startPosition(matcher.start())
                        .endPosition(matcher.end() - 1)
                        .build());
                startPos = matcher.end();
            }

            return found;
        } else if (pattern.getSearchType() == PatternSearchType.SUBSTRING) {
            String content = new String(bytes);
            int startSearch = 0;
            final String value = pattern.getValue();

            while (true) {
                int start = StringUtils.indexOfIgnoreCase(content, value, startSearch);

                if (start == -1) {
                    return found;
                }

                int end = start + value.length() - 1;
                found.add(FoundPattern.builder()
                        .patternId(pattern.getId())
                        .startPosition(start)
                        .endPosition(end)
                        .build());

                startSearch = end + 1;
            }
        } else {  // SUBBYTES
            int startSearch = 0;
            final byte[] value = Hex.decode(pattern.getValue());

            while (true) {
                int start = Bytes.indexOf(bytes, value, startSearch, bytes.length);

                if (start == -1) {
                    return found;
                }

                int end = start + value.length - 1;
                found.add(FoundPattern.builder()
                        .patternId(pattern.getId())
                        .startPosition(start)
                        .endPosition(end)
                        .build());

                startSearch = end + 1;
            }
        }
    }

    @Transactional
    public void deleteById(int id) {
        final Optional<Pattern> optional = repository.findById(id);
        if (optional.isPresent()) {
            final Pattern pattern = optional.get();
            log.info("Удален паттерн {} со значением {}", pattern.getName(), pattern.getValue());

            for (Stream stream : pattern.getMatchedStreams()) {
                stream.getFoundPatterns().remove(pattern);
                streamService.save(stream);
            }

            pattern.getMatchedStreams().clear();
            patterns.remove(id);
            compiledPatterns.remove(pattern.getValue());
            repository.delete(pattern);
            subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.DELETE_PATTERN, id));
        }
    }

    public Pattern save(Pattern pattern) {
        log.info("Добавлен новый паттерн {} со значением {}", pattern.getName(), pattern.getValue());
        final Pattern saved = repository.save(pattern);
        patterns.put(saved.getId(), pattern);
        subscriptionService.broadcast(new SubscriptionMessage(SubscriptionMessageType.SAVE_PATTERN, saved));
        return saved;
    }

    private java.util.regex.Pattern compilePattern(Pattern pattern) {
        return compiledPatterns.computeIfAbsent(pattern.getValue(), java.util.regex.Pattern::compile);
    }

}
