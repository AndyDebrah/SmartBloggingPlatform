package com.smartblog.graphql;

import com.smartblog.core.model.Review;
import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.ReviewJpaRepository;
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
 * Epic 4: GraphQL Controller for Review operations
 */
@Controller
@RequiredArgsConstructor
public class ReviewGraphQLController {
    private final ReviewJpaRepository reviewRepository;
    private final PostJpaRepository postRepository;
    private final UserJpaRepository userRepository;

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Review> reviewsByPost(@Argument Long postId) {
        return postRepository.findById(postId)
                .map(post -> reviewRepository.findByPost(post, PageRequest.of(0, 100)).getContent())
                .orElse(List.of());
    }

    @QueryMapping
    @Transactional(readOnly = true)
    public List<Review> reviewsByUser(@Argument Long userId) {
        return userRepository.findById(userId)
                .map(user -> reviewRepository.findByUser(user, PageRequest.of(0, 100)).getContent())
                .orElse(List.of());
    }

    @MutationMapping
    @Transactional
    public Review createReview(@Argument CreateReviewInput input) {
        var post = postRepository.findById(input.postId())
                .orElseThrow(() -> new RuntimeException("Post not found"));
        var user = userRepository.findById(input.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (reviewRepository.existsByPostAndUser(post, user)) {
            throw new RuntimeException("User already reviewed this post");
        }

        Review review = Review.builder()
                .post(post)
                .user(user)
                .rating(input.rating())
                .reviewText(input.comment())
                .createdAt(LocalDateTime.now())
                .build();
        return reviewRepository.save(review);
    }

    @MutationMapping
    @Transactional
    public Boolean deleteReview(@Argument Long id) {
        return reviewRepository.findById(id)
                .map(review -> {
                    review.softDelete();
                    reviewRepository.save(review);
                    return true;
                })
                .orElse(false);
    }

    public record CreateReviewInput(Integer rating, String comment, Long postId, Long userId) {}
}
