package ru.serega6531.packmate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@GenericGenerator(
        name = "packet_generator",
        strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "packet_seq"),
                @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                @org.hibernate.annotations.Parameter(name = "increment_size", value = "1")
        }
)
@AllArgsConstructor
@Builder
@Table(indexes = { @Index(name = "stream_id_index", columnList = "stream_id") })
public class Packet {

    @Id
    @GeneratedValue(generator = "packet_generator")
    private Long id;

    @Transient
    private Long tempId;

    @Transient
    private int ttl;

    @Column(name = "stream_id")
    @JsonIgnore
    private Long streamId;

    @OneToMany(mappedBy = "packet", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<FoundPattern> matches;

    private long timestamp;

    private boolean incoming; // true если от клиента к серверу, иначе false

    private boolean ungzipped;

    private boolean webSocketParsed;

    private boolean tlsDecrypted;

    private byte[] content;

    @Transient
    public String getContentString() {
        return new String(content);
    }

    public String toString() {
        return "Packet(id=" + id + ", content=" + getContentString() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Packet packet = (Packet) o;
        return id != null && Objects.equals(id, packet.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
