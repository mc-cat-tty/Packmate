package ru.serega6531.packmate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.serega6531.packmate.model.Pattern;
import ru.serega6531.packmate.model.PatternType;

import java.util.List;

public interface PatternRepository extends JpaRepository<Pattern, Integer> {

    List<Pattern> findAllByTypeEqualsOrTypeEquals(PatternType type, PatternType both);

}
