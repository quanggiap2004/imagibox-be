package com.imagibox.repository;

import com.imagibox.domain.entity.User;
import com.imagibox.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRole(UserRole role);

    @Query("SELECT u FROM User u WHERE u.parent.id = :parentId")
    List<User> findKidsByParentId(@Param("parentId") Long parentId);

    @Query("SELECT u.id FROM User u WHERE u.parent.id = :parentId")
    List<Long> findKidIdByParentId(@Param("parentId") Long parentId);

    @Query("SELECT u FROM User u WHERE u.role = 'KID' AND u.parent.id = :parentId")
    List<User> findAllKidsByParent(@Param("parentId") Long parentId);
}
