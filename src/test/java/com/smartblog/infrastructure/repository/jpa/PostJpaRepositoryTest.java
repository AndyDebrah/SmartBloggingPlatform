package com.smartblog.infrastructure.repository.jpa;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.smartblog.core.model.Post;
import com.smartblog.core.model.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class PostJpaRepositoryTest {

    @Autowired
    private PostJpaRepository postJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    public void testSearchByTitleOrContent() {
        User u = User.builder().username("tester").email("tester@example.com").passwordHash("testpwd").build();
        userJpaRepository.save(u);

        Post p = Post.builder().author(u).title("Hello World").content("This is a sample content").published(true).build();
        postJpaRepository.save(p);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> res = postJpaRepository.searchByTitleOrContent("hello", pageable);

        Assertions.assertFalse(res.isEmpty(), "Expected search to return at least one result");
        Assertions.assertEquals(1, res.getTotalElements());
    }

    @Test
    public void testFindByAuthorUsernameLike() {
        User u = User.builder().username("author123").email("a@example.com").passwordHash("authorpwd").build();
        userJpaRepository.save(u);

        Post p = Post.builder().author(u).title("Authored Post").content("Content").published(true).build();
        postJpaRepository.save(p);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> res = postJpaRepository.findByAuthorUsernameLike("author", pageable);

        Assertions.assertFalse(res.isEmpty());
        Assertions.assertEquals(1, res.getTotalElements());
    }
}
