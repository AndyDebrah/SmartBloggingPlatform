package com.smartblog.infrastructure.repository.jpa;

import com.smartblog.core.model.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagJpaRepository extends JpaRepository<Tag, Long> {

    /**
     * Find tag by slug (URL-friendly identifier)
     */
    Optional<Tag> findBySlug(String slug);

    /**
     * Find tag by name

     * @return Optional containing tag if found
     */
    Optional<Tag> findByName(String name);

    /**

     * @return true if tag exists
     */
    boolean existsByName(String name);

    /**

     * @return true if tag exists
     */
    boolean existsBySlug(String slug);

    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Tag> searchByName(@Param("name") String name, Pageable pageable);


    @Query("SELECT t, COUNT(p) as postCount FROM Tag t LEFT JOIN t.posts p " +
            "GROUP BY t.id " +
            "ORDER BY postCount DESC")
    List<Object[]> findTagsWithPostCount();


    @Query("SELECT t FROM Tag t LEFT JOIN t.posts p " +
            "GROUP BY t.id " +
            "ORDER BY COUNT(p) DESC")
    Page<Tag> findMostUsedTags(Pageable pageable);
}
