package ru.serega6531.packmate.model;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@GenericGenerator(
        name = "found_pattern_generator",
        strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "found_pattern_seq"),
                @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                @org.hibernate.annotations.Parameter(name = "increment_size", value = "1")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@ToString
@EqualsAndHashCode(exclude = "packet")
public class FoundPattern {

    @Id
    @GeneratedValue(generator = "found_pattern_generator")
    private int id;

    @ManyToOne
    @JoinColumn(name = "packet_id", nullable = false)
    @Setter
    private Packet packet;

    private int patternId;

    private int startPosition;

    private int endPosition;

}


