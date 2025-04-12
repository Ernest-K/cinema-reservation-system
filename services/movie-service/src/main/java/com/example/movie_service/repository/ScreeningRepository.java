package com.example.movie_service.repository;

import com.example.movie_service.entity.Screening;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {
    List<Screening> findAllByMovieId(Long movieId);
}