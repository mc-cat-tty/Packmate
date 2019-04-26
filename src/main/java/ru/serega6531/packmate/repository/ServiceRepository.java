package ru.serega6531.packmate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.serega6531.packmate.model.Service;

public interface ServiceRepository extends JpaRepository<Service, Integer> {
}
