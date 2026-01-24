package org.crisisconnect.controller;

import org.crisisconnect.dto.OrganizationResponse;
import org.crisisconnect.model.entity.Organization;
import org.crisisconnect.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Organization controller for managing aid organizations.
 */
@RestController
@RequestMapping("/organizations")
public class OrganizationController {

    @Autowired
    private OrganizationRepository organizationRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('NGO_STAFF', 'ADMIN')")
    public ResponseEntity<List<OrganizationResponse>> getAllOrganizations() {
        List<Organization> organizations = organizationRepository.findAll();
        List<OrganizationResponse> responses = organizations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private OrganizationResponse mapToResponse(Organization org) {
        OrganizationResponse response = new OrganizationResponse();
        response.setId(org.getId());
        response.setName(org.getName());
        response.setType(org.getType());
        response.setCountry(org.getCountry());
        response.setStatus(org.getStatus());
        response.setWebsiteUrl(org.getWebsiteUrl());
        response.setPhone(org.getPhone());
        response.setCreatedAt(org.getCreatedAt());
        return response;
    }
}
