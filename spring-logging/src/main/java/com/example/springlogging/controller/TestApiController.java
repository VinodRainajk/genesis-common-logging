package com.example.springlogging.controller;

import com.example.springlogging.dto.NodeRequest;
import genesis.common.logging.Tracking;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestApiController {

    private static final Logger log = LoggerFactory.getLogger(TestApiController.class);

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody NodeRequest request) {
        Tracking.init(request.getRunNodeID());
        log.info("CREATE received");
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<Void> get(@Valid @RequestBody NodeRequest request) {
        Tracking.init(request.getRunNodeID());
        log.info("GET received");
        return ResponseEntity.ok().build();
    }

    @PutMapping
    public ResponseEntity<Void> update(@Valid @RequestBody NodeRequest request) {
        Tracking.init(request.getRunNodeID());
        log.info("UPDATE received");
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@Valid @RequestBody NodeRequest request) {
        Tracking.init(request.getRunNodeID());
        log.info("DELETE received");
        return ResponseEntity.ok().build();
    }
}
