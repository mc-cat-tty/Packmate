package ru.serega6531.packmate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.model.Pagination;
import ru.serega6531.packmate.model.Stream;
import ru.serega6531.packmate.service.ServicesService;
import ru.serega6531.packmate.service.StreamService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/stream/")
public class StreamController {

    private final StreamService streamService;
    private final ServicesService servicesService;

    @Autowired
    public StreamController(StreamService streamService, ServicesService servicesService) {
        this.streamService = streamService;
        this.servicesService = servicesService;
    }

    @PostMapping("/all")
    public List<Stream> getStreams(@RequestBody Pagination pagination) {
        return streamService.findAll(pagination);
    }

    @PostMapping("/{port}")
    public List<Stream> getStreams(@PathVariable int port, @RequestBody Pagination pagination) {
        final Optional<CtfService> serviceOptional = servicesService.findByPort(port);
        if(serviceOptional.isPresent()) {
            return streamService.findAllByService(pagination, serviceOptional.get());
        } else {
            return Collections.emptyList();
        }
    }

}
