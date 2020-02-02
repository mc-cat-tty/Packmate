package ru.serega6531.packmate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.serega6531.packmate.model.Stream;
import ru.serega6531.packmate.model.pojo.Pagination;
import ru.serega6531.packmate.service.StreamService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/stream/")
public class StreamController {

    private final StreamService streamService;

    @Autowired
    public StreamController(StreamService streamService) {
        this.streamService = streamService;
    }

    @PostMapping("/all")
    public List<Stream> getStreams(@RequestBody Pagination pagination) {
        return streamService.findAll(pagination, Optional.empty(), pagination.isFavorites());
    }

    @PostMapping("/{port}")
    public List<Stream> getStreams(@PathVariable int port, @RequestBody Pagination pagination) {
        return streamService.findAll(pagination, Optional.of(port), pagination.isFavorites());
    }

    @PostMapping("/{id}/favorite")
    public void favoriteStream(@PathVariable long id) {
        streamService.setFavorite(id, true);
    }

    @PostMapping("/{id}/unfavorite")
    public void unfavoriteStream(@PathVariable long id) {
        streamService.setFavorite(id, false);
    }

}
