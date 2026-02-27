package com.smartblog.web;

import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/form")
public class FormCsrfController {

    @GetMapping(value = "/csrf", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> csrfForm(HttpServletRequest request, CsrfToken token) {
        String paramName = token.getParameterName();
        String tokenValue = token.getToken();

        String html = "<!doctype html>\n" +
                "<html><head><title>CSRF Demo</title></head><body>\n" +
                "<h3>CSRF-protected Form</h3>\n" +
                "<form method=\"post\" action=\"/form/submit\">\n" +
                "<input type=\"hidden\" name=\"" + paramName + "\" value=\"" + tokenValue + "\"/>\n" +
                "<label>message: <input name=\"message\" /></label>\n" +
                "<button type=\"submit\">Submit</button>\n" +
                "</form>\n" +
                "</body></html>";

        return ResponseEntity.ok().body(html);
    }

    @PostMapping("/submit")
    public ResponseEntity<String> submitForm(@RequestParam(required = false) String message) {
        String out = "received:" + (message == null ? "" : message);
        return ResponseEntity.created(URI.create("/form/submit")).body(out);
    }
}
