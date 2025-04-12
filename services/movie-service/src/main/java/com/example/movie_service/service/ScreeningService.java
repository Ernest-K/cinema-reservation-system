package com.example.movie_service.service;

import com.example.movie_service.dto.ScreeningResponse;
import com.example.movie_service.entity.Screening;
import com.example.movie_service.repository.ScreeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ScreeningService {

    private final ScreeningRepository screeningRepository;

    public List<ScreeningResponse> getScreeningsByMovieId(Long movieId) {
        return screeningRepository.findAllByMovieId(movieId).stream().map(this::mapToScreeningResponse).collect(Collectors.toList());
    }

    private ScreeningResponse mapToScreeningResponse(Screening screening) {
        return new ScreeningResponse(screening.getId(), screening.getStartTime(), screening.getBasePrice(), screening.getMovie());
    }
}
