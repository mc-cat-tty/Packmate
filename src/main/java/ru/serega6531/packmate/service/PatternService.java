package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.repository.PatternRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PatternService {

    private final PatternRepository repository;

    private Map<String, java.util.regex.Pattern> compiledPatterns = new HashMap<>();

    @Autowired
    public PatternService(PatternRepository repository) {
        this.repository = repository;
    }

    public List<Pattern> findAll() {
        return repository.findAll();
    }

    public List<Pattern> findMatching(byte[] bytes) {
        String content = new String(bytes);

        return findAll().stream()
                .filter(pattern -> matches(pattern, content))
                .collect(Collectors.toList());
    }

    private boolean matches(Pattern pattern, String content) {
        if(pattern.isRegex()) {
            final java.util.regex.Pattern regex = compilePattern(pattern);
            return regex.matcher(content).find();
        } else {
            return StringUtils.containsIgnoreCase(content, pattern.getValue());
        }
    }

    public void deleteById(int id) {
        repository.deleteById(id);
    }

    public Pattern save(Pattern pattern) {
        log.info("Добавлен новый паттерн {} со значением {}", pattern.getName(), pattern.getValue());
        return repository.save(pattern);
    }

    public java.util.regex.Pattern compilePattern(Pattern pattern) {
        return compiledPatterns.computeIfAbsent(pattern.getValue(), java.util.regex.Pattern::compile);
    }

}
