
package com.smartblog.application.service;

import com.smartblog.application.util.SlugUtil;
import com.smartblog.core.dto.TagDTO;
import com.smartblog.core.exceptions.DuplicateException;
import com.smartblog.core.exceptions.NotFoundException;
import com.smartblog.core.exceptions.ValidationException;
import com.smartblog.core.mapper.TagMapper;
import com.smartblog.core.model.Tag;
import com.smartblog.infrastructure.repository.api.TagRepository;

import java.util.List;

public class TagServiceImpl implements TagService {
    private final TagRepository tags;
    public TagServiceImpl(TagRepository tags) { this.tags = tags; }

    @Override
    public long create(String name) {
        validateName(name);
        String slug = SlugUtil.toSlug(name);
        if (tags.findBySlug(slug).isPresent()) throw new DuplicateException("Tag slug already exists: " + slug);
        Tag t = new Tag(null, name.trim(), slug);
        try { return tags.create(t); }
        catch (RuntimeException r) {
            if ("duplicate".equals(r.getMessage())) throw new DuplicateException("Tag already exists");
            throw r;
        }
    }

    @Override
    public boolean rename(long tagId, String newName) {
        validateName(newName);
        var t = tags.findById(tagId).orElseThrow(() -> new NotFoundException("Tag not found"));
        String newSlug = SlugUtil.toSlug(newName);
        if (tags.findBySlug(newSlug).filter(x -> !x.getId().equals(tagId)).isPresent())
            throw new DuplicateException("Slug already in use");
        t.setName(newName.trim()); t.setSlug(newSlug);
        return tags.update(t);
    }
    @Override
    public List<TagDTO> list() {
        return tags.listAll().stream()
                .map(tag -> new TagDTO(tag.getId(), tag.getName()))
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .toList();
    }

    @Override public boolean delete(long tagId) { return tags.delete(tagId); }
    @Override public List<TagDTO> listAll() { return tags.listAll().stream().map(TagMapper::toDTO).toList(); }
    @Override public boolean assignToPost(long postId, long tagId) { return tags.addTagToPost(postId, tagId); }
    @Override public boolean removeFromPost(long postId, long tagId) { return tags.removeTagFromPost(postId, tagId); }
    @Override public List<TagDTO> listForPost(long postId) { return tags.listByPost(postId).stream().map(TagMapper::toDTO).toList(); }

    private void validateName(String name) {
        if (name == null || name.isBlank()) throw new ValidationException("Tag name required");
        if (name.length() > 100) throw new ValidationException("Tag name too long");
    }
}
