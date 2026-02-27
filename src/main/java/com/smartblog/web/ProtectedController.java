package com.smartblog.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/protected")
public class ProtectedController {

    @GetMapping("/admin-test")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminTest() {
        return "admin-access";
    }

    @GetMapping("/author-test")
    @PreAuthorize("hasAnyRole('AUTHOR','ADMIN')")
    public String authorTest() {
        return "author-access";
    }

    @GetMapping("/reader-test")
    @PreAuthorize("hasAnyRole('READER','AUTHOR','ADMIN')")
    public String readerTest() {
        return "reader-access";
    }
}
