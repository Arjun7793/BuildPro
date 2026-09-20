package com.example.buildpro.repository;

import com.example.buildpro.entity.Lead;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

// Builds the WHERE clause for GET /api/leads' optional name/date-range filters -
// one Specification per filter, composed with .and() in LeadServiceImpl for
// whichever ones were actually supplied.
//
// Every method here returns Specification.unrestricted() (a no-op, always-true
// specification) rather than null when its filter wasn't supplied - as of
// Spring Data JPA 4.0, Specification.and(other) rejects a null "other" outright
// (Assert.notNull, "Other specification must not be null"), so a method
// couldn't return null anymore and still be handed straight into .and().
// unrestricted() is the framework's own null-safe stand-in for exactly this.
public final class LeadSpecifications {

    private LeadSpecifications() {
    }

    public static Specification<Lead> nameContains(String name) {
        if (name == null || name.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + name.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    public static Specification<Lead> createdOnOrAfter(LocalDateTime from) {
        if (from == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Lead> createdBefore(LocalDateTime exclusiveUpperBound) {
        if (exclusiveUpperBound == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.lessThan(root.get("createdAt"), exclusiveUpperBound);
    }
}
