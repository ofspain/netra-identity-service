package com.netra.authrex.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;

import com.netra.commons.models.RoleTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RoleTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private RoleTemplate sampleTemplate;

    @BeforeEach
    void setup() {
        sampleTemplate = new RoleTemplate();
        sampleTemplate.setName("Default Template");
        sampleTemplate.setDescription("Contains base roles");
    }

    @Test
    void testCreateTemplate_success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/role-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTemplate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();

        System.out.println("Response JSON: " + result.getResponse().getContentAsString());
    }

    @Test
    void testSearchTemplates_success() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/role-templates")
                        .param("pageNum", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andReturn();

        System.out.println("Response JSON: " + result.getResponse().getContentAsString());
    }

    @Test
    void testGetTemplate_notFound() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/role-templates/{id}", 9999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"))
                .andReturn();

        System.out.println("Response JSON: " + result.getResponse().getContentAsString());
    }
}

