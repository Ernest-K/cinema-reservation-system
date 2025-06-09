package com.example.reservation_service.repository;

import com.example.reservation_service.entity.ScreeningSeatInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreeningSeatInfoRepository extends JpaRepository<ScreeningSeatInfo, Long> {
    List<ScreeningSeatInfo> findAllByScreeningInfoId(Long screeningCopyId);
    Optional<ScreeningSeatInfo> findByScreeningInfoIdAndOriginalSeatId(Long screeningCopyId, Long originalSeatId);
    List<ScreeningSeatInfo> findAllByScreeningInfoIdAndOriginalSeatIdIn(Long screeningCopyId, List<Long> originalSeatIds);

    boolean existsByScreeningInfoIdAndOriginalSeatId(Long id, Long id1);

//    List<ScreeningSeatInfo> findAllByScreeningInfoIdAndOriginalSeatIdIn(Long id, List<Long> seatIds);
//
//    Optional<ScreeningSeatInfo> findByScreeningInfoIdAndOriginalSeatId(Long id, Long seatId);
//
//    List<ScreeningSeatInfo> findAllByScreeningInfoId(Long id);

    void deleteAllByScreeningInfoId(Long id);
}