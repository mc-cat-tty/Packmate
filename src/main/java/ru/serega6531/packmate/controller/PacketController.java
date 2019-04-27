package ru.serega6531.packmate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.service.PacketService;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/packet/")
public class PacketController {

    private final PacketService service;

    @Autowired
    public PacketController(PacketService service) {
        this.service = service;
    }

    @PostMapping("/all")
    public List<Packet> getPackets() {
        return Collections.emptyList();
    }

    @PostMapping("/{stream}")
    public List<Packet> getStreams(@PathVariable int stream) {
        return Collections.emptyList();
    }

}
