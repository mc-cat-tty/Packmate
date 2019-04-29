package ru.serega6531.packmate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import ru.serega6531.packmate.Protocol;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@GenericGenerator(
        name = "stream_generator",
        strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "stream_seq"),
                @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                @org.hibernate.annotations.Parameter(name = "increment_size", value = "1")
        }
)
public class Stream {

    @Id
    @GeneratedValue(generator = "stream_generator")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private CtfService service;

    private Protocol protocol;

    @OneToMany(mappedBy = "stream", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Packet> packets;

    private long startTimestamp;

    private long endTimestamp;

}
