package ru.serega6531.packmate.model;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "service")
public class CtfService {

    @Id
    private int port;

    private String name;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Stream> streams;

}