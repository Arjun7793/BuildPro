package com.example.buildpro.entity;

// The two kinds of sample plan a project showcase item can be (see
// SamplePlan.planType). Stored as its name (EnumType.STRING) rather than an
// ordinal, so the sample_plans.plan_type column stays readable and reordering
// this enum later can't silently corrupt existing rows.
public enum PlanType {
    SKETCH_2D,
    ANIMATION_3D
}
