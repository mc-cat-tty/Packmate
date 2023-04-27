package ru.serega6531.packmate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.model.pojo.PatternDto;
import ru.serega6531.packmate.service.PatternService;

import java.util.List;

@RestController
@RequestMapping("/api/pattern/")
public class PatternController {

    private final PatternService service;

    @Autowired
    public PatternController(PatternService service) {
        this.service = service;
    }

    @GetMapping
    public List<PatternDto> getPatterns() {
        return service.findAll()
                .stream().map(service::toDto)
                .toList();
    }

    @PostMapping("/{id}")
    public void enable(@PathVariable int id, @RequestParam boolean enabled) {
        service.enable(id, enabled);
    }

    @PostMapping("/{id}/lookback")
    public void lookBack(@PathVariable int id, @RequestBody int minutes) {
        if (minutes < 1) {
            return;
        }

        service.lookBack(id, minutes);
    }

    @PostMapping
    public PatternDto addPattern(@RequestBody PatternDto dto) {
        dto.setEnabled(true);
        Pattern pattern = service.fromDto(dto);
        Pattern saved = service.save(pattern);
        return service.toDto(saved);
    }

}
