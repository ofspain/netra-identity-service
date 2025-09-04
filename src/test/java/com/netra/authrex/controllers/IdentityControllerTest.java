package com.netra.authrex.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netra.authrex.dtos.ChangePasswordRequest;
import com.netra.authrex.dtos.IdentityRoleDto;
import com.netra.authrex.dtos.IdentitySearchParam;
import com.netra.authrex.dtos.IdentityWithRolesDto;
import com.netra.authrex.services.IdentityService;
import com.netra.commons.models.Identity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = IdentityController.class)
class IdentityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private ObjectMapper objectMapper;

    private Identity identity;

    @BeforeEach
    void setup() {
        identity = new Identity();
        identity.setId(1L);
        identity.setUsername("johndoe");
    }

    @Test
    void testCreateIdentity() throws Exception {
        Mockito.when(identityService.createIdentity(any(Identity.class)))
                .thenReturn(identity);

        mockMvc.perform(post("/api/identities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(identity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void testUpdateIdentity() throws Exception {
        identity.setUsername("updatedUser");
        Mockito.when(identityService.updateIdentity(any(Identity.class)))
                .thenReturn(identity);

        mockMvc.perform(put("/api/identities/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(identity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updatedUser"));
    }

    @Test
    void testGetIdentityById_found() throws Exception {
        Mockito.when(identityService.findIdentityById(1L))
                .thenReturn(Optional.of(identity));

        mockMvc.perform(get("/api/identities/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void testGetIdentityById_notFound() throws Exception {
        Mockito.when(identityService.findIdentityById(1L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/identities/{id}", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetIdentityByUsername_found() throws Exception {
        Mockito.when(identityService.findIdentityByUsername("johndoe"))
                .thenReturn(Optional.of(identity));

        mockMvc.perform(get("/api/identities/username/{username}", "johndoe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void testSearchIdentities() throws Exception {
        Page<IdentityWithRolesDto> page = new PageImpl<>(List.of(new IdentityWithRolesDto()));
        Mockito.when(identityService.findIdentities(any(IdentitySearchParam.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/identities/search")
                        .param("pageNum", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testAssignRoleToIdentity() throws Exception {
        IdentityRoleDto roleDto = new IdentityRoleDto();
        roleDto.setRoleId(2L);

        doNothing().when(identityService).assignRoleToIdentity(1L, 2L);

        mockMvc.perform(post("/api/identities/{id}/roles", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleDto)))
                .andExpect(status().isOk());

        verify(identityService).assignRoleToIdentity(1L, 2L);
    }

    @Test
    void testRemoveRoleFromIdentity() throws Exception {
        doNothing().when(identityService).removeRoleFromIdentity(1L, 2L);

        mockMvc.perform(delete("/api/identities/{id}/roles/{roleId}", 1L, 2L))
                .andExpect(status().isOk());

        verify(identityService).removeRoleFromIdentity(1L, 2L);
    }

    @Test
    void testAssignRoleTemplateToIdentity() throws Exception {
        IdentityRoleDto roleDto = new IdentityRoleDto();
        roleDto.setRoleId(5L);

        doNothing().when(identityService).assignRoleTemplateToIdentity(1L, 5L);

        mockMvc.perform(post("/api/identities/{id}/role-templates", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleDto)))
                .andExpect(status().isOk());

        verify(identityService).assignRoleTemplateToIdentity(1L, 5L);
    }

    @Test
    void testRemoveRoleTemplateFromIdentity() throws Exception {
        doNothing().when(identityService).removeRoleTemplateFromIdentity(1L, 10L);

        mockMvc.perform(delete("/api/identities/{id}/role-templates/{templateId}", 1L, 10L))
                .andExpect(status().isOk());

        verify(identityService).removeRoleTemplateFromIdentity(1L, 10L);
    }

    @Test
    void testChangePassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword("newPass123");

        doNothing().when(identityService).changePassword(1L, "newPass123");

        mockMvc.perform(post("/api/identities/{id}/change-password", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(identityService).changePassword(1L, "newPass123");
    }
}

