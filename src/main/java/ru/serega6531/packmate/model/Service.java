package ru.serega6531.packmate.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class Service {

    @Id
    private int port;
    private String name;

}
