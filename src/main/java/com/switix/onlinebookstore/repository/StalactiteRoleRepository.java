package com.switix.onlinebookstore.repository;

import com.switix.onlinebookstore.model.AppUser;
import com.switix.onlinebookstore.model.Role;
import org.codefilarete.stalactite.spring.repository.StalactiteRepository;

import java.util.Optional;

public interface StalactiteRoleRepository extends StalactiteRepository<Role, Long> {

    Optional<Role> findByName(String name);
}
