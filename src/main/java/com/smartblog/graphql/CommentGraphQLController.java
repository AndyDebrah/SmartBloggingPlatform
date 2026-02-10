package com.smartblog.graphql;

import com.smartblog.core.model.Comment;
import com.smartblog.infrastructure.repository.jpa.CommentJpaRepository;
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
 * Epic 4: GraphQL Controller for Comment operations
 */
@Controller
@RequiredArgsConstructor
public class CommentGraphQLController {
    private final CommentJpaRepository commentRepository;
    private final PostJpaRepository postRepository;
    private final UserJpaRepository userRepository;

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Comment> commentsByPost(@Argument Long postId) {
        return postRepository.findById(postId)
                .map(post -> commentRepository.findByPost(post, PageRequest.of(0, 100)).getContent())
                .orElse(List.of());
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Comment> commentsByUser(@Argument Long userId) {
        return userRepository.findById(userId)
                .map(user -> commentRepository.findByUser(user, PageRequest.of(0, 100)).getContent())
                .orElse(List.of());
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Comment> recentComments(@Argument int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return commentRepository.findRecentComments(since, PageRequest.of(0, 100)).getContent();
    }

    @MutationMapping
    @Transactional
    public Comment createComment(@Argument CreateCommentInput input) {
        var post = postRepository.findById(input.postId())
                .orElseThrow(() -> new RuntimeException("Post not found"));
        var user = userRepository.findById(input.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(input.content())
                .createdAt(LocalDateTime.now())
                .build();
        return commentRepository.save(comment);
    }

    @MutationMapping
    @Transactional
    public Boolean deleteComment(@Argument Long id) {
        return commentRepository.findById(id)
                .map(comment -> {
                    comment.softDelete();
                    commentRepository.save(comment);
                    return true;
                })
                .orElse(false);
    }

    public record CreateCommentInput(String content, Long postId, Long userId) {}
}
