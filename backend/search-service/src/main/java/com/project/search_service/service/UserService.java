package com.project.search_service.service;

import com.project.search_service.domain.dto.SearchUserDto;
import com.project.search_service.domain.dto.UserSearchResponseDto;
import com.project.search_service.domain.entity.Users;
import com.project.search_service.repository.UsersRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final UsersRepository usersRepository;
    private static final int PAGE_LENGTH = 12;

    @Transactional
    public void createUser(SearchUserDto requestDto) {
        Users users = Users
                .builder()
                .id(requestDto.getId().toString())
                .username(requestDto.getUsername())
                .displayName(requestDto.getDisplayName())
                .bio(requestDto.getBio())
                .enable(true)
                .build();

        log.info("User with id : {} and username : {} is Created", users.getId().toString(), users.getUsername());
        usersRepository.save(users);
    }

    @Transactional
    public void updateUser(SearchUserDto requestDto) {
        Users user = usersRepository.findById(requestDto.getId().toString()).orElse(null);

        if (user == null) {
            log.info("User : {} not found", requestDto.getId());
            return;
        }

        List<String> updated = new ArrayList<>();

        if (requestDto.getUsername() != null) {
            user.setUsername(requestDto.getUsername());
            updated.add("Username");
        }

        if (requestDto.getDisplayName() != null) {
            user.setDisplayName(requestDto.getDisplayName());
            updated.add("Display Name");
        }

        if (requestDto.getBio() != null) {
            user.setBio(requestDto.getBio());
            updated.add("Bio");
        }

        usersRepository.save(user);
        log.info("User : {} ({}) update : {}", user.getId(), user.getUsername(), updated.toString());
    }

    public Page<UserSearchResponseDto> searchUser(String keyword, int page) {

        if (page < 0) {
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, PAGE_LENGTH);
        return usersRepository.searchByKeyword(keyword, pageable);
    }

}
