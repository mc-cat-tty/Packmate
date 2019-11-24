package ru.serega6531.packmate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.serega6531.packmate.model.Pagination;
import ru.serega6531.packmate.model.Stream;
import ru.serega6531.packmate.service.StreamService;

import java.util.List;

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
        if (pagination.isFavorites()) {
            return streamService.findFavorites(pagination);
        } else {
            return streamService.findAll(pagination);
        }
    }

    @PostMapping("/{port}")
    public List<Stream> getStreams(@PathVariable int port, @RequestBody Pagination pagination) {
        if (pagination.isFavorites()) {
            return streamService.findFavoritesByService(pagination, port);
        } else {
            return streamService.findAllByService(pagination, port);
        }
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
