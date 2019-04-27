package ru.serega6531.packmate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serega6531.packmate.model.Stream;
import ru.serega6531.packmate.service.StreamService;

import java.util.List;

@RestController
@RequestMapping("/api/stream/")
public class StreamController {

    private final StreamService service;

    @Autowired
    public StreamController(StreamService service) {
        this.service = service;
    }

    @GetMapping("/all")
    public List<Stream> getStreams() {
        return service.findAll();
    }

    @GetMapping("/{port}")
    public List<Stream> getStreams(@PathVariable int port) {
        return service.findAllByServicePort(port);
    }

}
