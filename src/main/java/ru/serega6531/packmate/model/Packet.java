package ru.serega6531.packmate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Packet {

    @Id
    @GeneratedValue(generator = "packet_generator")
    private Long id;

    @Transient
    @JsonIgnore
    private Long tempId;

    @ManyToOne
    @JoinColumn(name = "stream_id", nullable = false)
    @JsonIgnore
    private Stream stream;

    private long timestamp;

    private byte[] content;

}
