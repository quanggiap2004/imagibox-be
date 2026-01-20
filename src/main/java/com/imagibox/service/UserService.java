package com.imagibox.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.imagibox.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public List<Long> findKidsByParentId(Long parentId) {
        return userRepository.findKidIdByParentId(parentId);
    }

}
