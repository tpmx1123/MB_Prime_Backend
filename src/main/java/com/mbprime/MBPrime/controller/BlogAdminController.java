package com.mbprime.MBPrime.controller;

import com.mbprime.MBPrime.entity.Blog;
import com.mbprime.MBPrime.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/blogs")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173", "https://mbprimeprojects.com", "https://www.mbprimeprojects.com"}, allowCredentials = "true")
public class BlogAdminController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private final BlogRepository blogRepository;

    @Autowired
    public BlogAdminController(BlogRepository blogRepository) {
        this.blogRepository = blogRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated."));
        }
        List<Blog> list = blogRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> body = list.stream().map(this::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated."));
        }
        Optional<Blog> opt = blogRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toMap(opt.get()));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> payload, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated."));
        }
        String slug = stringOrEmpty(payload.get("slug"));
        if (slug.isBlank()) slug = slugify(stringOrEmpty(payload.get("title")));
        if (slug.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Title or slug is required."));
        }
        if (blogRepository.existsBySlug(slug)) {
            return ResponseEntity.badRequest().body(Map.of("message", "A blog with this slug already exists."));
        }
        Blog blog = mapToBlog(payload, new Blog());
        blog.setSlug(slug);
        blog = blogRepository.save(blog);
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(blog));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> payload, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated."));
        }
        Optional<Blog> opt = blogRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Blog blog = opt.get();
        String slug = stringOrEmpty(payload.get("slug"));
        if (slug.isBlank()) slug = slugify(stringOrEmpty(payload.get("title")));
        if (slug.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Title or slug is required."));
        }
        if (blogRepository.existsBySlugAndIdNot(slug, id)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Another blog with this slug already exists."));
        }
        blog.setSlug(slug);
        mapToBlog(payload, blog);
        blog = blogRepository.save(blog);
        return ResponseEntity.ok(toMap(blog));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated."));
        }
        if (!blogRepository.existsById(id)) return ResponseEntity.notFound().build();
        blogRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private Blog mapToBlog(Map<String, Object> payload, Blog blog) {
        blog.setTitle(stringOrEmpty(payload.get("title")));
        blog.setCategory(stringOrEmpty(payload.get("category")));
        blog.setAuthor(stringOrEmpty(payload.get("author")));
        blog.setExcerpt(stringOrEmpty(payload.get("excerpt")));
        blog.setBody(stringOrEmpty(payload.get("body")));
        blog.setImage(stringOrEmpty(payload.get("image")));
        blog.setCarouselImage(stringOrEmpty(payload.get("carouselImage")));
        return blog;
    }

    private Map<String, Object> toMap(Blog b) {
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
            Map.entry("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : ""),
            Map.entry("updatedAt", b.getUpdatedAt() != null ? b.getUpdatedAt().toString() : "")
        );
    }

    private static String stringOrEmpty(Object o) {
        if (o == null) return "";
        String s = o.toString();
        return s != null ? s.trim() : "";
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private static String slugify(String title) {
        if (title == null || title.isBlank()) return "";
        return title.trim().toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("[\\s_-]+", "-")
            .replaceAll("^-|-$", "");
    }
}
