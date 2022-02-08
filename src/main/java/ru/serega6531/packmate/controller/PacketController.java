package ru.serega6531.packmate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.serega6531.packmate.model.Packet;
import ru.serega6531.packmate.model.pojo.PacketDto;
import ru.serega6531.packmate.model.pojo.PacketPagination;
import ru.serega6531.packmate.service.StreamService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/packet/")
public class PacketController {

    private final StreamService streamService;

    @Autowired
    public PacketController(StreamService streamService) {
        this.streamService = streamService;
    }

    @PostMapping("/{streamId}")
    public List<PacketDto> getPacketsForStream(@PathVariable long streamId, @RequestBody PacketPagination pagination) {
        List<Packet> packets = streamService.getPackets(streamId, pagination.getStartingFrom(), pagination.getPageSize());
        return packets.stream()
                .map(streamService::packetToDto)
                .collect(Collectors.toList());
    }

}
