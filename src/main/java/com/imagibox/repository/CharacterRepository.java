package com.imagibox.repository;

import com.imagibox.domain.entity.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Long> {

    List<Character> findByUserId(Long userId);

    long countByUserId(Long userId);
}
