package com.example.buildpro.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// A sample plan shown under Projects on the public site - either a 2D sketch
// (an image) or a 3D animation (a video link, with this same image field
// standing in as its thumbnail/poster). Deliberately flat, standalone rows -
// not linked to any one ProjectItem - same as every other content entity in
// this app; see ProjectItem for the identical image-upload comments this
// entity mirrors.
@Entity
@Table(name = "sample_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SamplePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "planType is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 20)
    private PlanType planType;

    @NotBlank(message = "title is required")
    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    // Either an external URL, or - once an image has been uploaded through the
    // admin page - set automatically to this plan's own /api/sample-plans/{id}/image
    // endpoint (see SamplePlanController). Required for a 2D Sketch (it IS the
    // sketch); for a 3D Animation it's the thumbnail shown before the video link
    // is opened. See the identical comment on ProjectItem.imageUrl.
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    // The uploaded image's raw bytes, if any - never serialized to JSON. Served
    // separately, raw, by GET /api/sample-plans/{id}/image. See ProjectItem.
    @JsonIgnore
    @Column(name = "image_data", columnDefinition = "bytea")
    private byte[] imageData;

    @Column(name = "image_content_type")
    private String imageContentType;

    // External link to the 3D walkthrough/animation - a YouTube/Vimeo URL or a
    // direct video file. Only meaningful for planType ANIMATION_3D; left null
    // for a SKETCH_2D plan. Not stored as bytes like the image above - videos
    // are large enough that embedding a link is far cheaper than storing the
    // file in Postgres, and this app has no other video-hosting story.
    @Column(name = "video_url", length = 1000)
    private String videoUrl;

    @Column(name = "display_order")
    private Integer displayOrder;

    // Draft/published staging flag - see the identical field on ProjectItem for
    // the full explanation (filtering, default, and why @NotNull rather than
    // nullable).
    @NotNull(message = "published is required")
    @Column(nullable = false)
    private Boolean published = Boolean.TRUE;
}
