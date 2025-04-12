package com.example.movie_service.repository;

import com.example.movie_service.entity.Hall;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HallRespository extends JpaRepository<Hall, Long> {}
