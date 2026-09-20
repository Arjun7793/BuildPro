package com.example.buildpro.service.impl;

import com.example.buildpro.entity.Testimonial;
import com.example.buildpro.repository.TestimonialRepository;
import com.example.buildpro.service.TestimonialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TestimonialServiceImpl implements TestimonialService {

    private final TestimonialRepository testimonialRepository;

    @Override
    public List<Testimonial> findAll() {
        return testimonialRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Override
    public Optional<Testimonial> findById(Long id) {
        return testimonialRepository.findById(id);
    }

    @Override
    public Testimonial create(Testimonial testimonial) {
        return testimonialRepository.save(testimonial);
    }

    @Override
    public Optional<Testimonial> update(Long id, Testimonial update) {
        return testimonialRepository.findById(id).map(existing -> {
            existing.setClientName(update.getClientName());
            existing.setMessage(update.getMessage());
            existing.setRating(update.getRating());
            existing.setDisplayOrder(update.getDisplayOrder());
            return testimonialRepository.save(existing);
        });
    }

    @Override
    public boolean delete(Long id) {
        if (!testimonialRepository.existsById(id)) {
            return false;
        }
        testimonialRepository.deleteById(id);
        return true;
    }
}
