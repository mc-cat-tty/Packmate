package ru.serega6531.packmate.model;

import lombok.Data;
import org.springframework.data.domain.Sort;

@Data
public class Pagination {

    private boolean fetchLatest;

    private Sort.Direction direction;

    private long startingFrom;

    private int pageSize;

}
