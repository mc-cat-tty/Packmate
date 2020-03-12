package ru.serega6531.packmate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import ru.serega6531.packmate.model.enums.Protocol;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Data
@ToString(exclude = "packets")
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

    @Column(name = "service_id")
    private int service;

    private Protocol protocol;

    @OneToMany(mappedBy = "stream", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Packet> packets;

    private long startTimestamp;

    private long endTimestamp;

    @ManyToMany(cascade = CascadeType.ALL)
    private Set<Pattern> foundPatterns;

    private boolean favorite;

    private byte ttl;

    @Column(columnDefinition = "char(3)")
    private String userAgentHash;

}
