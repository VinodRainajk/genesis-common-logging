package com.example.springlogging.controller;

import genesis.common.logging.Tracking;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestApiController.class)
class TestApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String BODY = """
            {
              "runNodeID": "node-1",
              "correlationId": "corr-123",
              "address": "10.0.0.1"
            }
            """;

    @AfterEach
    void tearDown() {
        Tracking.clear();
    }

    @Test
    void postLogsAndReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));
    }

    @Test
    void getLogsAndReturnsOk() throws Exception {
        mockMvc.perform(get("/api/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void putLogsUpdateWithoutDml() throws Exception {
        mockMvc.perform(put("/api/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void deleteLogsDeleteWithoutDml() throws Exception {
        mockMvc.perform(delete("/api/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}
