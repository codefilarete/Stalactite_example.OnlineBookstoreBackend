package com.switix.onlinebookstore.repository;

import com.switix.onlinebookstore.model.AppUser;
import org.codefilarete.stalactite.spring.repository.StalactiteRepository;

import java.util.Optional;

public interface StalactiteAppUserRepository extends StalactiteRepository<AppUser,Long> {

    Optional<AppUser> findByEmail(String email);
}
