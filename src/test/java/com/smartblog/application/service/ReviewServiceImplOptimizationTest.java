package com.smartblog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.smartblog.application.service.impl.ReviewServiceImpl;
import com.smartblog.core.exceptions.NotFoundException;
import com.smartblog.core.model.Post;
import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.ReviewJpaRepository;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplOptimizationTest {

    @Mock
    private ReviewJpaRepository reviewRepository;

    @Mock
    private PostJpaRepository postRepository;

    @Mock
    private UserJpaRepository userRepository;

    private ReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(reviewRepository, postRepository, userRepository);
    }

    @Test
    void getPostRatingStats_singleQueryEnabled_shouldUseSummaryQueryOnly() {
        ReflectionTestUtils.setField(service, "singleQueryReviewStatsEnabled", true);

        ReviewJpaRepository.PostRatingSummaryProjection projection =
                new ReviewJpaRepository.PostRatingSummaryProjection() {
                    @Override
                    public Long getPostId() {
                        return 10L;
                    }

                    @Override
                    public Double getAverageRating() {
                        return 4.26;
                    }

                    @Override
                    public Long getReviewCount() {
                        return 7L;
                    }
                };

        when(reviewRepository.findPostRatingSummary(10L)).thenReturn(Optional.of(projection));

        var result = service.getPostRatingStats(10L);

        assertThat(result.get("postId")).isEqualTo(10L);
        assertThat(result.get("averageRating")).isEqualTo(4.3);
        assertThat(result.get("reviewCount")).isEqualTo(7L);

        verify(reviewRepository).findPostRatingSummary(10L);
        verify(postRepository, never()).findById(any());
        verify(reviewRepository, never()).calculateAverageRating(any(Post.class));
        verify(reviewRepository, never()).countByPostAndDeletedAtIsNull(any(Post.class));
    }

    @Test
    void getPostRatingStats_singleQueryDisabled_shouldUseLegacyPath() {
        ReflectionTestUtils.setField(service, "singleQueryReviewStatsEnabled", false);
        Post post = Post.builder().title("t").content("c").published(true).build();

        when(postRepository.findById(12L)).thenReturn(Optional.of(post));
        when(reviewRepository.calculateAverageRating(post)).thenReturn(3.0);
        when(reviewRepository.countByPostAndDeletedAtIsNull(post)).thenReturn(9L);

        var result = service.getPostRatingStats(12L);

        assertThat(result.get("postId")).isEqualTo(12L);
        assertThat(result.get("averageRating")).isEqualTo(3.0);
        assertThat(result.get("reviewCount")).isEqualTo(9L);

        verify(postRepository).findById(12L);
        verify(reviewRepository).calculateAverageRating(post);
        verify(reviewRepository).countByPostAndDeletedAtIsNull(post);
        verify(reviewRepository, never()).findPostRatingSummary(any());
    }

    @Test
    void getPostRatingStats_singleQueryEnabled_shouldThrowWhenMissingPost() {
        ReflectionTestUtils.setField(service, "singleQueryReviewStatsEnabled", true);
        when(reviewRepository.findPostRatingSummary(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPostRatingStats(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Post not found");
    }
}

