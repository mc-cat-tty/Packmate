package ru.serega6531.packmate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@GenericGenerator(
        name = "pattern_generator",
        strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
        parameters = {
                @org.hibernate.annotations.Parameter(name = "sequence_name", value = "pattern_seq"),
                @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                @org.hibernate.annotations.Parameter(name = "increment_size", value = "1")
        }
)
public class Pattern {

    @Id
    @GeneratedValue(generator = "pattern_generator")
    private int id;

    private String name;

    private String value;

    private String color;  // для вставки в css

    @ManyToMany(mappedBy = "foundPatterns", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Stream> matchedStreams;

}
