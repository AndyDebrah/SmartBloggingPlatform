package com.smartblog.graphql;

import com.smartblog.core.model.Tag;
import com.smartblog.infrastructure.repository.jpa.TagJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Epic 4: GraphQL Controller for Tag operations
 */
@Controller
@RequiredArgsConstructor
public class TagGraphQLController {
    private final TagJpaRepository tagRepository;

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Tag> allTags() {
        return tagRepository.findAll();
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public Tag tag(@Argument Long id) {
        return tagRepository.findById(id).orElse(null);
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public Tag tagBySlug(@Argument String slug) {
        return tagRepository.findBySlug(slug).orElse(null);
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Tag> popularTags(@Argument int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return tagRepository.findMostUsedTags(PageRequest.of(0, limit)).getContent();
    }
}
