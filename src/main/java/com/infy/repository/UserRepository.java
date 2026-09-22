package com.infy.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.infy.entity.User;

/**
 * JpaRepository (rather than plain CrudRepository) is used deliberately:
 * Phase 2's getAllUsers() needs Pageable support, which JpaRepository already
 * provides via PagingAndSortingRepository.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
