package com.smartblog.graphql;

import com.smartblog.core.model.Post;
import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Epic 4: GraphQL Controller for Post operations
 */
@Controller
@RequiredArgsConstructor
public class PostGraphQLController {
    private final PostJpaRepository postRepository;
    private final UserJpaRepository userRepository;

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Post> allPosts() {
        return postRepository.findAll();
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public Post post(@Argument Long id) {
        return postRepository.findById(id).orElse(null);
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Post> postsByAuthor(@Argument Long authorId) {
        return userRepository.findById(authorId)
                .map(author -> postRepository.findPublishedByAuthor(author, PageRequest.of(0, 100)).getContent())
                .orElse(List.of());
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Post> publishedPosts() {
        return postRepository.findAllPublished(PageRequest.of(0, 100)).getContent();
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Post> recentPosts(@Argument int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return postRepository.findRecentPosts(since, PageRequest.of(0, 100)).getContent();
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Post> searchPosts(@Argument String query) {
        return postRepository.searchByFullText(query, PageRequest.of(0, 100)).getContent();
    }

    @MutationMapping
    @Transactional
    public Post createPost(@Argument CreatePostInput input) {
        return userRepository.findById(input.authorId())
                .map(author -> {
                    Post post = Post.builder()
                            .title(input.title())
                            .content(input.content())
                            .author(author)
                            .published(input.published() != null ? input.published() : false)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return postRepository.save(post);
                })
                .orElseThrow(() -> new RuntimeException("Author not found"));
    }

    @MutationMapping
    @Transactional
    public Post updatePost(@Argument Long id, @Argument UpdatePostInput input) {
        return postRepository.findById(id)
                .map(post -> {
                    if (input.title() != null) post.setTitle(input.title());
                    if (input.content() != null) post.setContent(input.content());
                    if (input.published() != null) post.setPublished(input.published());
                    post.setUpdatedAt(LocalDateTime.now());
                    return postRepository.save(post);
                })
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    @MutationMapping
    @Transactional
    public Boolean deletePost(@Argument Long id) {
        return postRepository.findById(id)
                .map(post -> {
                    post.softDelete();
                    postRepository.save(post);
                    return true;
                })
                .orElse(false);
    }

    public record CreatePostInput(String title, String content, Long authorId, Boolean published) {}
    public record UpdatePostInput(String title, String content, Boolean published) {}
}