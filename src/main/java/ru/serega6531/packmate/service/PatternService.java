package ru.serega6531.packmate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.repository.PatternRepository;

import java.util.List;

@Service
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

    public Pattern save(Pattern service) {
        return repository.save(service);
    }

}
