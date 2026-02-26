package com.mbprime.MBPrime.controller;

import com.mbprime.MBPrime.entity.Blog;
import com.mbprime.MBPrime.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/blogs")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173", "https://mbprimeprojects.com", "https://www.mbprimeprojects.com"}, allowCredentials = "true")
public class BlogController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private final BlogRepository blogRepository;

    @Autowired
    public BlogController(BlogRepository blogRepository) {
        this.blogRepository = blogRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        List<Blog> list = blogRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> body = list.stream().map(this::toPublicMap).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> getBySlug(@PathVariable String slug) {
        Optional<Blog> opt = blogRepository.findBySlug(slug);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toPublicMap(opt.get()));
    }

    private Map<String, Object> toPublicMap(Blog b) {
        String dateStr = b.getCreatedAt() != null ? b.getCreatedAt().format(DATE_FORMAT) : "";
        return Map.<String, Object>ofEntries(
            Map.entry("id", b.getId()),
            Map.entry("title", nullToEmpty(b.getTitle())),
            Map.entry("slug", nullToEmpty(b.getSlug())),
            Map.entry("category", nullToEmpty(b.getCategory())),
            Map.entry("author", nullToEmpty(b.getAuthor())),
            Map.entry("excerpt", nullToEmpty(b.getExcerpt())),
            Map.entry("body", nullToEmpty(b.getBody())),
            Map.entry("image", nullToEmpty(b.getImage())),
            Map.entry("carouselImage", b.getCarouselImage() != null && !b.getCarouselImage().isBlank() ? b.getCarouselImage() : nullToEmpty(b.getImage())),
            Map.entry("date", dateStr),
            Map.entry("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : "")
        );
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
