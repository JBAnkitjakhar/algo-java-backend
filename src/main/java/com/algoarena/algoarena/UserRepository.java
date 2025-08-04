//src/main/java/com/algoarena/algoarena/UserRepository.java

package com.algoarena.algoarena;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    // Find user by provider ID (Google ID or GitHub ID)
    Optional<User> findByProviderId(String providerId);
    
    // Find user by provider and provider ID
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    
    // Find user by email (useful for Google users)
    Optional<User> findByEmail(String email);
    
    // Find user by username (useful for GitHub users)
    Optional<User> findByUsername(String username);
    
    // Check if user exists by provider ID
    boolean existsByProviderId(String providerId);
}