package ru.serega6531.packmate.model;

import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import ru.serega6531.packmate.model.enums.Protocol;

import javax.persistence.*;
import java.util.*;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@GenericGenerator(
        name = "stream_generator",
        strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @Parameter(name = "sequence_name", value = "stream_seq"),
                @Parameter(name = "initial_value", value = "1"),
                @Parameter(name = "increment_size", value = "1")
        }
)
public class Stream {

    @Id
    @GeneratedValue(generator = "stream_generator")
    private Long id;

    @Column(name = "service_id")
    private int service;

    private Protocol protocol;

    @OneToMany(mappedBy = "stream", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    @ToString.Exclude
    private List<Packet> packets;

    private long startTimestamp;

    private long endTimestamp;

    @ManyToMany
    @JoinTable(
            name = "stream_found_patterns",
            joinColumns = @JoinColumn(name = "stream_id"),
            inverseJoinColumns = @JoinColumn(name = "pattern_id")
    )
    @ToString.Exclude
    private Set<Pattern> foundPatterns = new HashSet<>();

    private boolean favorite;

    @Column(columnDefinition = "smallint")
    private int ttl;

    @Column(columnDefinition = "char(3)")
    private String userAgentHash;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Stream stream = (Stream) o;
        return id != null && Objects.equals(id, stream.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
