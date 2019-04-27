package ru.serega6531.packmate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.repository.PatternRepository;

import java.util.List;

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

    public void deleteById(int id) {
        repository.deleteById(id);
    }

    public Pattern save(Pattern pattern) {
        log.info("Добавлен новый паттерн {} со значением {}", pattern.getName(), pattern.getValue());
        return repository.save(pattern);
    }

}
