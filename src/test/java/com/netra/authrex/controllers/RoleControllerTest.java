package com.netra.authrex.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netra.authrex.dtos.*;

import com.netra.commons.models.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Role sampleRole;

    @BeforeEach
    void setup() {
        sampleRole = new Role();
        sampleRole.setName("ADMIN");
        sampleRole.setDescription("Administrator role");
    }

    @Test
    void testCreateRole_success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value(sampleRole.getName()))
                .andReturn();

        System.out.println("Response JSON: " + result.getResponse().getContentAsString());
    }

    @Test
    void testSearchRoles_success() throws Exception {
        RoleAndTemplateSearchParam searchParam =
                new RoleAndTemplateSearchParam().withPageNum(0).withPageSize(10);


        MvcResult result = mockMvc.perform(get("/api/roles")
                        .param("pageNum", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andReturn();

        System.out.println("Response JSON: " + result.getResponse().getContentAsString());
    }

    @Test
    void testGetRole_notFound() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/roles/{id}", 9999))
                .andExpect(status().isOk()) // because ApiResponse.error still wraps response in 200
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("404"))
                .andReturn();

        System.out.println("Response JSON: " + result.getResponse().getContentAsString());
    }
}

