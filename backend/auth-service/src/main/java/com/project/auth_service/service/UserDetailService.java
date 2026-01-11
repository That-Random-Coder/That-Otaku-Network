package com.project.auth_service.service;

import com.project.auth_service.domain.entity.UserProfile;
import com.project.auth_service.repository.UserProfileRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class UserDetailService implements UserDetailsService {

    private final UserProfileRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            UserProfile userProfile = userRepo.findByUsernameOrEmail(username , username);
            if(userProfile == null){
                throw new UsernameNotFoundException(username);
            }
            return new UserDetail(userProfile);

        } catch (Exception e) {
            log.error("User Detail Service : {} " ,e.getMessage());
            throw new UsernameNotFoundException(username);
        }
    }

    public UserDetails loadUserById(UUID userId) {
        try {
            UserProfile userProfile = userRepo.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException(userId.toString()));

            return new UserDetail(userProfile);

        } catch (Exception e) {
            log.error("User Detail Service : {} " ,e.getMessage());
            throw new UsernameNotFoundException(userId.toString());
        }
    }
}
