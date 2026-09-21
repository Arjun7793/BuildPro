package com.example.buildpro.service.impl;

import com.example.buildpro.dto.LeadStats;
import com.example.buildpro.entity.Lead;
import com.example.buildpro.repository.LeadRepository;
import com.example.buildpro.repository.LeadSpecifications;
import com.example.buildpro.service.LeadNotificationService;
import com.example.buildpro.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final LeadNotificationService leadNotificationService;

    @Override
    public Page<Lead> search(String name, LocalDate from, LocalDate to, Pageable pageable) {
        // "to" is inclusive of the whole day from the caller's point of view, so
        // the actual upper bound used in the query is the start of the next day.
        //
        // Specification.where(null) is gone as of Spring Data JPA 4.0 - it no
        // longer accepts null and would throw "Specification must not be null"
        // as soon as any filter (name/from/to) wasn't supplied. unrestricted()
        // is the replacement: a null-safe, always-true starting point that
        // .and(...) can chain onto, where each null filter (see
        // LeadSpecifications - each returns null when its input is null) is
        // simply elided rather than contributing a predicate.
        Specification<Lead> spec = Specification.<Lead>unrestricted()
                .and(LeadSpecifications.nameContains(name))
                .and(LeadSpecifications.createdOnOrAfter(from != null ? from.atStartOfDay() : null))
                .and(LeadSpecifications.createdBefore(to != null ? to.plusDays(1).atStartOfDay() : null));

        // A Specification-based query doesn't inherit the repository method name's
        // implicit ordering the unfiltered endpoint used to have, so fall back to
        // newest-first explicitly when the caller didn't ask for a specific sort.
        Pageable effective = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        return leadRepository.findAll(spec, effective);
    }

    @Override
    public Optional<Lead> findById(Long id) {
        return leadRepository.findById(id);
    }

    @Override
    public Lead create(Lead lead) {
        lead.setId(null);
        lead.setCreatedAt(null);
        Lead saved = leadRepository.save(lead);
        // Best-effort - see LeadNotificationServiceImpl for why a notification
        // failure never propagates back up to fail this (already-successful)
        // contact form submission.
        leadNotificationService.notifyNewLead(saved);
        return saved;
    }

    @Override
    public boolean delete(Long id) {
        if (!leadRepository.existsById(id)) {
            return false;
        }
        leadRepository.deleteById(id);
        return true;
    }

    @Override
    public LeadStats getStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now()
                .with(WeekFields.of(Locale.getDefault()).getFirstDayOfWeek())
                .atTime(LocalTime.MIN);

        long total = leadRepository.count();
        long today = leadRepository.countByCreatedAtGreaterThanEqual(todayStart);
        long thisWeek = leadRepository.countByCreatedAtGreaterThanEqual(weekStart);

        return new LeadStats(total, today, thisWeek);
    }
}
