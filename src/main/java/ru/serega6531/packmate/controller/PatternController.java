package ru.serega6531.packmate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.service.PatternService;

import java.util.Collection;

@RestController
@RequestMapping("/api/pattern/")
public class PatternController {

    private final PatternService service;

    @Autowired
    public PatternController(PatternService service) {
        this.service = service;
    }

    @GetMapping
    public Collection<Pattern> getPatterns() {
        return service.findAll();
    }

    @DeleteMapping("/{id}")
    public void deletePattern(@PathVariable int id) {
        service.deleteById(id);
    }

    @PostMapping
    public Pattern addPattern(@RequestBody Pattern pattern) {
        return service.save(pattern);
    }

}
