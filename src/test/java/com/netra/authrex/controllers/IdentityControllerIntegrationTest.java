package com.netra.authrex.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.netra.authrex.dtos.*;
import com.netra.authrex.services.IdentityService;
import com.netra.commons.enums.DomainType;
import com.netra.commons.models.Identity;
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
class IdentityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private ObjectMapper objectMapper;

    private Identity testIdentity;

    @BeforeEach
    void setup() {
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String username = "johndoe";
        try{
            AuthUser old = (AuthUser) identityService.loadUserByUsername(username);
            testIdentity = old.getIdentity();
            System.out.println(objectMapper.writeValueAsString(old));
            System.out.println(".....");
        }catch(Exception ex){
            ex.printStackTrace();
            Identity identity = new Identity();
            identity.setUsername(username);
            identity.setPassword("password123");
            identity.setDomainType(DomainType.FINANCIAL_INSTITUTION);
            identity.setDomainCode("FBN");
            testIdentity = identityService.createIdentity(identity);

            System.out.println("freshly created ");
        }


    }

    @Test
    void testCreateIdentity() throws Exception {
        String username = "vdm@phyna";
        Identity newIdentity = new Identity();
        newIdentity.setUsername(username);
        newIdentity.setPassword("secret123");
     //   newIdentity.setDomainType(DomainType.FINANCIAL_INSTITUTION); // should be overridden
        newIdentity.setDomainCode("FBN"); // should be overridden

        MvcResult result = mockMvc.perform(post("/api/identities/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newIdentity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.username").value(username))
                // overridden values must be CUSTOMER_USER / CUSTOMER
                .andExpect(jsonPath("$.data.domainCode").value(Identity.CUSTOMERUSER_DOMAINCODE))
                .andExpect(jsonPath("$.data.domainType").value(DomainType.CUSTOMER.name()))
                .andReturn();

// print the actual JSON response body
        String responseBody = result.getResponse().getContentAsString();
        System.out.println("Response JSON: " + responseBody);

    }

    @Test
    void testUpdateIdentity() throws Exception {
        testIdentity.setUsername("updatedUser");

        mockMvc.perform(put("/api/identities/{id}", testIdentity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testIdentity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updatedUser"));
    }

    @Test
    void testGetIdentityById_found() throws Exception {
        mockMvc.perform(get("/api/identities/{id}", testIdentity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void testGetIdentityById_notFound() throws Exception {
        mockMvc.perform(get("/api/identities/{id}", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetIdentityByUsername_found() throws Exception {
        mockMvc.perform(get("/api/identities/username/{username}", "johndoe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testIdentity.getId()));
    }

    @Test
    void testSearchIdentities() throws Exception {
        mockMvc.perform(get("/api/identities/search")
                        .param("pageNum", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testAssignRoleToIdentity() throws Exception {
        IdentityRoleDto roleDto = new IdentityRoleDto();
        roleDto.setRoleId(2L); // assumes role with id 2 exists in DB

        mockMvc.perform(post("/api/identities/{id}/roles", testIdentity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleDto)))
                .andExpect(status().isOk());
    }

    @Test
    void testRemoveRoleFromIdentity() throws Exception {
        mockMvc.perform(delete("/api/identities/{id}/roles/{roleId}", testIdentity.getId(), 2L))
                .andExpect(status().isOk());
    }

    @Test
    void testAssignRoleTemplateToIdentity() throws Exception {
        IdentityRoleDto roleDto = new IdentityRoleDto();
        roleDto.setRoleId(5L); // assumes role template with id 5 exists

        mockMvc.perform(post("/api/identities/{id}/role-templates", testIdentity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleDto)))
                .andExpect(status().isOk());
    }

    @Test
    void testRemoveRoleTemplateFromIdentity() throws Exception {
        mockMvc.perform(delete("/api/identities/{id}/role-templates/{templateId}", testIdentity.getId(), 10L))
                .andExpect(status().isOk());
    }

    @Test
    void testChangePassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword("newPass123");

        mockMvc.perform(post("/api/identities/{id}/change-password", testIdentity.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
