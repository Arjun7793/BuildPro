package com.example.buildpro.service;

import com.example.buildpro.entity.Testimonial;

import java.util.List;
import java.util.Optional;

public interface TestimonialService {
    List<Testimonial> findAll();
    Optional<Testimonial> findById(Long id);
    Testimonial create(Testimonial testimonial);
    Optional<Testimonial> update(Long id, Testimonial update);
    boolean delete(Long id);
}
