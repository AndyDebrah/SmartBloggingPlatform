package com.smartblog.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.smartblog.core.model.Post;
import com.smartblog.core.model.User;
import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

@SpringBootTest(classes = com.smartblog.SmartBlogApplication.class)
@ActiveProfiles("test")
public class PropagationTransactionTest {

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PostJpaRepository postJpaRepository;

    @Autowired
    private OuterTestService outerService;

    private User author;

    @BeforeEach
    void setup() {
        userJpaRepository.deleteAll();
        postJpaRepository.deleteAll();
        author = User.builder().username("propuser").email("prop@example.com").passwordHash("p").build();
        userJpaRepository.save(author);
    }

    @Test
    public void innerCommitsWhenOuterRollsBack() {
        try {
            outerService.outerCreatesAndCallsInnerThenRollback(author.getId());
        } catch (RuntimeException ignored) {
            // expected
        }

        // Inner (REQUIRES_NEW) should be committed, outer's save should be rolled back
        assertEquals(1, postJpaRepository.findByAuthorId(author.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements());
        assertTrue(postJpaRepository.findByAuthorId(author.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getContent().stream().anyMatch(p -> "inner".equals(p.getTitle())));
    }

    @Test
    public void bothCommitWhenNoException() {
        outerService.outerCreatesAndCallsInner(author.getId());

        assertEquals(2, postJpaRepository.findByAuthorId(author.getId(), org.springframework.data.domain.PageRequest.of(0, 10)).getTotalElements());
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @Bean
        public InnerTestService innerTestService(PostJpaRepository pr, UserJpaRepository ur) {
            return new InnerTestService(pr, ur);
        }

        @Bean
        public OuterTestService outerTestService(PostJpaRepository pr, InnerTestService inner, UserJpaRepository ur) {
            return new OuterTestService(pr, inner, ur);
        }
    }

    public static class InnerTestService {
        private final PostJpaRepository postRepo;
        private final UserJpaRepository userRepo;

        public InnerTestService(PostJpaRepository postRepo, UserJpaRepository userRepo) {
            this.postRepo = postRepo;
            this.userRepo = userRepo;
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void createInner(long authorId) {
            User a = userRepo.findById(authorId).orElseThrow();
            Post p = Post.builder().author(a).title("inner").content("inner").published(false).build();
            postRepo.save(p);
        }
    }

    public static class OuterTestService {
        private final PostJpaRepository postRepo;
        private final InnerTestService inner;
        private final UserJpaRepository userRepo;

        public OuterTestService(PostJpaRepository postRepo, InnerTestService inner, UserJpaRepository userRepo) {
            this.postRepo = postRepo;
            this.inner = inner;
            this.userRepo = userRepo;
        }

        @Transactional
        public void outerCreatesAndCallsInner(long authorId) {
            User a = userRepo.findById(authorId).orElseThrow();
            Post p = Post.builder().author(a).title("outer").content("outer").published(false).build();
            postRepo.save(p);
            inner.createInner(authorId);
        }

        @Transactional
        public void outerCreatesAndCallsInnerThenRollback(long authorId) {
            User a = userRepo.findById(authorId).orElseThrow();
            Post p = Post.builder().author(a).title("outer").content("outer").published(false).build();
            postRepo.save(p);
            // call inner which uses REQUIRES_NEW and should commit
            inner.createInner(authorId);
            // force rollback of outer transaction
            throw new RuntimeException("force outer rollback");
        }
    }
}
