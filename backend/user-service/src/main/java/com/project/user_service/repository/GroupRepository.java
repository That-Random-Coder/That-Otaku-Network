package com.project.user_service.repository;

import com.project.user_service.domain.dto.response.GroupDetailResponseDto;
import com.project.user_service.domain.dto.response.GroupResponseDto;
import com.project.user_service.domain.entity.groups.Group;
import com.project.user_service.domain.entity.groups.ImageGroup;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

        @Query("SELECT new com.project.user_service.domain.dto.response.GroupResponseDto(" +
                        "g.id, " +
                        "g.groupName, " +
                        "g.bio, " +
                        "g.leader.username, " +
                        "g.leader.id, " +
                        "g.leader.displayName, " +
                        "SIZE(g.members) + 1, " +
                        "g.dateOfCreation, " +
                        "(CASE WHEN g.leader.id = :userId OR :userId IN (SELECT m.id FROM g.members m) THEN true ELSE false END),"
                        +
                        "null, null, null, null) " +
                        "FROM Group g WHERE g.id = :id AND g.enable = true")
        Optional<GroupResponseDto> getGroupById(@Param("id") UUID id, @Param("userId") UUID userId);

        @Query("""
                            SELECT ig
                            FROM Group g
                            JOIN g.images ig
                            WHERE g.id = :id AND g.enable = true
                        """)
        ImageGroup getGroupImageById(@Param("id") UUID id);

        @Modifying
        @Query("UPDATE Group g SET g.bio = :bio WHERE g.id = :id AND g.enable = true")
        int updateTheBio(@Param("id") UUID id, @Param("bio") String bio);

        @Query("SELECT g.leader.id FROM Group g WHERE g.id = :id")
        UUID getGroupLeaderId(@Param("id") UUID id);

        @Query("SELECT g.leader.id FROM Group g WHERE g.id IN :id")
        Set<UUID> getAllGroupLeaderId(@Param("id") Set<UUID> id);

}
