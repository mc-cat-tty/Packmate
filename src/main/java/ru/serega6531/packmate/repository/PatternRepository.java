package ru.serega6531.packmate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.model.enums.PatternDirectionType;

import java.util.List;

public interface PatternRepository extends JpaRepository<Pattern, Integer> {

    List<Pattern> findAllByTypeEqualsOrTypeEquals(PatternDirectionType type, PatternDirectionType both);

}
