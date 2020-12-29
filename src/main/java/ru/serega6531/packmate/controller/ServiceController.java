package ru.serega6531.packmate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.serega6531.packmate.model.CtfService;
import ru.serega6531.packmate.model.pojo.ServiceDto;
import ru.serega6531.packmate.service.ServicesService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/service/")
public class ServiceController {

    private final ServicesService service;

    @Autowired
    public ServiceController(ServicesService service) {
        this.service = service;
    }

    @GetMapping
    public List<ServiceDto> getServices() {
        return service.findAll().stream()
                .map(service::toDto)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/{port}")
    public void deleteService(@PathVariable int port) {
        service.deleteByPort(port);
    }

    @PostMapping
    public CtfService addService(@RequestBody ServiceDto dto) {
        CtfService newService = this.service.fromDto(dto);
        return this.service.save(newService);
    }

}
