package ru.serega6531.packmate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.serega6531.packmate.model.pojo.StreamPagination;
import ru.serega6531.packmate.model.pojo.StreamDto;
import ru.serega6531.packmate.service.StreamService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stream/")
public class StreamController {

    private final StreamService service;

    @Autowired
    public StreamController(StreamService service) {
        this.service = service;
    }

    @PostMapping("/all")
    public List<StreamDto> getStreams(@RequestBody StreamPagination pagination) {
        return service.findAll(pagination, Optional.empty(), pagination.isFavorites()).stream()
                .map(service::streamToDto)
                .collect(Collectors.toList());
    }

    @PostMapping("/{port}")
    public List<StreamDto> getStreams(@PathVariable int port, @RequestBody StreamPagination pagination) {
        return service.findAll(pagination, Optional.of(port), pagination.isFavorites()).stream()
                .map(service::streamToDto)
                .collect(Collectors.toList());
    }

    @PostMapping("/{id}/favorite")
    public void favoriteStream(@PathVariable long id) {
        service.setFavorite(id, true);
    }

    @PostMapping("/{id}/unfavorite")
    public void unfavoriteStream(@PathVariable long id) {
        service.setFavorite(id, false);
    }

}
