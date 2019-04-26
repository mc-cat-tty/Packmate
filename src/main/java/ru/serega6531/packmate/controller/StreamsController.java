package ru.serega6531.packmate.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serega6531.packmate.model.Stream;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/service/")
public class StreamsController {

    @GetMapping("/all")
    public List<Stream> getStreams() {
        return Collections.emptyList();
    }

    @GetMapping("/{port}")
    public List<Stream> getStreams(@PathVariable int port) {
        return Collections.emptyList();
    }

}
