package ru.serega6531.packmate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.serega6531.packmate.model.Packet;
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
    public List<Packet> getPacketsForStream(@PathVariable long streamId) {
        final Optional<Stream> stream = streamService.find(streamId);
        if (stream.isPresent()) {
            return packetService.getPacketsForStream(stream.get());
        } else {
            return Collections.emptyList();
        }
    }

}
