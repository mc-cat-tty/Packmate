package ru.serega6531.packmate.model;

import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import ru.serega6531.packmate.model.enums.Protocol;

import javax.persistence.*;
import java.util.HashSet;
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
    @OrderBy("id")
    private List<Packet> packets;

    private long startTimestamp;

    private long endTimestamp;

    @ManyToMany
    private Set<Pattern> foundPatterns = new HashSet<>();

    private boolean favorite;

    @Column(columnDefinition = "smallint")
    private int ttl;

    @Column(columnDefinition = "char(3)")
    private String userAgentHash;

}
