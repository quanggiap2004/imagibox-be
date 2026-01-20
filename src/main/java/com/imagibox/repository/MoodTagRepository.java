package com.imagibox.repository;

import com.imagibox.domain.entity.MoodTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface MoodTagRepository extends JpaRepository<MoodTag, Long> {

        List<MoodTag> findByChapterId(Long chapterId);

        @Query("SELECT m.moodTag AS mood, COUNT(m) AS count " +
                        "FROM MoodTag m " +
                        "JOIN m.chapter c " +
                        "JOIN c.story s " +
                        "WHERE s.user.id = :userId " +
                        "GROUP BY m.moodTag " +
                        "ORDER BY COUNT(m) DESC")
        List<Map<String, Object>> getMoodDistributionByUserId(@Param("userId") Long userId);

        @Query("SELECT m.moodTag AS mood, COUNT(m) AS count " +
                        "FROM MoodTag m " +
                        "JOIN m.chapter c " +
                        "JOIN c.story s " +
                        "WHERE s.user.id IN :userIds " +
                        "GROUP BY m.moodTag " +
                        "ORDER BY COUNT(m) DESC")
        List<Map<String, Object>> getMoodDistributionByUserIdIn(@Param("userIds") List<Long> userIds);

        @Query("SELECT m FROM MoodTag m " +
                        "JOIN m.chapter c " +
                        "JOIN c.story s " +
                        "WHERE s.user.id = :userId " +
                        "AND m.createdAt BETWEEN :startDate AND :endDate")
        List<MoodTag> findByUserIdAndDateRange(
                        @Param("userId") Long userId,
                        @Param("startDate") OffsetDateTime startDate,
                        @Param("endDate") OffsetDateTime endDate);
}
