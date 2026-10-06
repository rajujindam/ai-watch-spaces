package com.aiwatchspaces.ai_watch_spaces.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "titles")
@Getter
@Setter
@NoArgsConstructor
public class Title {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title name is required")
    @Size(max = 200, message = "Title name must not exceed 200 characters")
    @Column(nullable = false, length = 200)
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be greater than zero")
    @Column(nullable = false)
    private Integer durationSeconds;

    @ElementCollection
    @CollectionTable(
            name = "title_genres",
            joinColumns = @JoinColumn(name = "title_id")
    )
    @Column(name = "genre")
    private Set<String> genres = new HashSet<>();

    public Title(String name, String description, Integer durationSeconds) {
        this.name = name;
        this.description = description;
        this.durationSeconds = durationSeconds;
    }
}