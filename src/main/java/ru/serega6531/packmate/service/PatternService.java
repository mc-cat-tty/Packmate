package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.repository.PatternRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PatternService {

    private final PatternRepository repository;

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
                .filter(pattern -> StringUtils.containsIgnoreCase(content, pattern.getValue()))
                .collect(Collectors.toList());
    }

    public void deleteById(int id) {
        repository.deleteById(id);
    }

    public Pattern save(Pattern pattern) {
        log.info("Добавлен новый паттерн {} со значением {}", pattern.getName(), pattern.getValue());
        return repository.save(pattern);
    }

}
