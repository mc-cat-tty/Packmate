package ru.serega6531.packmate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.model.Pagination;
import ru.serega6531.packmate.model.Stream;
import ru.serega6531.packmate.service.PacketService;
import ru.serega6531.packmate.service.StreamService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/packet/")
public class PacketController {

    private final StreamService streamService;
    private final PacketService packetService;

    @Autowired
    public PacketController(StreamService streamService, PacketService packetService) {
        this.streamService = streamService;
        this.packetService = packetService;
    }

    @PostMapping("/{streamId}")
    public List<Packet> getPacketsForStream(@PathVariable long streamId, @RequestBody Pagination pagination) {
        final Optional<Stream> stream = streamService.find(streamId);
        if(stream.isPresent()) {
            return packetService.getPacketsForStream(pagination, stream.get());
        } else {
            return Collections.emptyList();
        }
    }

}
