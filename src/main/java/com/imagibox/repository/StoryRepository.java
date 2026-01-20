package com.imagibox.repository;

import com.imagibox.domain.entity.Story;
import com.imagibox.domain.enums.StoryMode;
import com.imagibox.domain.enums.StoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    Page<Story> findByUserId(Long userId, Pageable pageable);

    Page<Story> findByUserIdIn(List<Long> userIds, Pageable pageable);

    List<Story> findByUserIdAndStatus(Long userId, StoryStatus status);

    List<Story> findByUserIdAndMode(Long userId, StoryMode mode);

    @Query("SELECT s FROM Story s WHERE s.user.id IN :userIds AND s.createdAt BETWEEN :startDate AND :endDate")
    List<Story> findStoriesByUserAndDateRange(
            @Param("userIds") List<Long> userIds,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

    @Query("SELECT COUNT(s) FROM Story s WHERE s.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(s) FROM Story s WHERE s.user.id IN :userIds")
    long countByUserIdIn(@Param("userIds") List<Long> userIds);

}
