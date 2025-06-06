package com.example.reservation_service.repository;

import com.example.reservation_service.entity.ScreeningInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreeningInfoRepository extends JpaRepository<ScreeningInfo, Long> {
    List<ScreeningInfo> findByMovieIdAndIsActiveTrue(Long movieId);
}
