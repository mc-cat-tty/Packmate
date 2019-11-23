package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.model.PatternType;
import ru.serega6531.packmate.model.Stream;
import ru.serega6531.packmate.repository.PatternRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PatternService {

    private final PatternRepository repository;
    private final StreamService streamService;

    private final Map<Integer, Pattern> patterns = new HashMap<>();
    private final Map<String, java.util.regex.Pattern> compiledPatterns = new HashMap<>();

    @Autowired
    public PatternService(PatternRepository repository, @Lazy StreamService streamService) {
        this.repository = repository;
        this.streamService = streamService;
        repository.findAll().forEach(p -> patterns.put(p.getId(), p));
        log.info("Loaded {} patterns", patterns.size());
    }

    public Collection<Pattern> findAll() {
        return patterns.values();
    }

    public List<Pattern> findMatching(byte[] bytes, boolean incoming) {
        String content = new String(bytes);

        return patterns.values().stream()
                .filter(p -> p.getType() == (incoming ? PatternType.INPUT : PatternType.OUTPUT)
                        || p.getType() == PatternType.BOTH)
                .filter(pattern -> matches(pattern, content))
                .collect(Collectors.toList());
    }

    private boolean matches(Pattern pattern, String content) {
        if (pattern.isRegex()) {
            final java.util.regex.Pattern regex = compilePattern(pattern);
            return regex.matcher(content).find();
        } else {
            return StringUtils.containsIgnoreCase(content, pattern.getValue());
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
            patterns.remove(pattern.getId());
            compiledPatterns.remove(pattern.getValue());
            repository.delete(pattern);
        }
    }

    public Pattern save(Pattern pattern) {
        log.info("Добавлен новый паттерн {} со значением {}", pattern.getName(), pattern.getValue());
        patterns.put(pattern.getId(), pattern);
        return repository.save(pattern);
    }

    private java.util.regex.Pattern compilePattern(Pattern pattern) {
        return compiledPatterns.computeIfAbsent(pattern.getValue(), java.util.regex.Pattern::compile);
    }

}
