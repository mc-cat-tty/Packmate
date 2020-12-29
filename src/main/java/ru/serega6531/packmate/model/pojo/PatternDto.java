package ru.serega6531.packmate.model.pojo;

import lombok.Data;
import ru.serega6531.packmate.model.enums.PatternDirectionType;
import ru.serega6531.packmate.model.enums.PatternSearchType;

@Data
public class PatternDto {

    private int id;
    private boolean enabled;
    private String name;
    private String value;
    private String color;  // для вставки в css
    private PatternSearchType searchType;
    private PatternDirectionType directionType;

}
