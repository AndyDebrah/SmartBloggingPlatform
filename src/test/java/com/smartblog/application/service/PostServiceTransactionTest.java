package com.smartblog.application.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.smartblog.core.model.Post;
import com.smartblog.core.model.User;
import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

@SpringBootTest(classes = com.smartblog.SmartBlogApplication.class)
@ActiveProfiles("test")
public class PostServiceTransactionTest {

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PostJpaRepository postJpaRepository;

    @Autowired
    private TestTransactionalService txService;

    @Test
    public void testRollbackOnRuntimeException() {
        User u = User.builder().username("txuser").email("tx@example.com").passwordHash("p").build();
        userJpaRepository.save(u);

        try {
            txService.saveThenThrow(u.getId());
        } catch (RuntimeException ex) {
            // expected
        }

        // Ensure post was not persisted due to rollback
        assertTrue(postJpaRepository.findByAuthorId(u.getId(), PageRequest.of(0, 10)).isEmpty());
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @Bean
        public TestTransactionalService testTransactionalService(UserJpaRepository ur, PostJpaRepository pr) {
            return new TestTransactionalService(ur, pr);
        }
    }

    public static class TestTransactionalService {
        private final UserJpaRepository userRepo;
        private final PostJpaRepository postRepo;

        public TestTransactionalService(UserJpaRepository userRepo, PostJpaRepository postRepo) {
            this.userRepo = userRepo;
            this.postRepo = postRepo;
        }

        @Transactional
        public void saveThenThrow(long authorId) {
            User author = userRepo.findById(authorId).orElseThrow();
            Post p = Post.builder().author(author).title("tx").content("tx").published(false).build();
            postRepo.save(p);
            throw new RuntimeException("force rollback");
        }
    }
}
