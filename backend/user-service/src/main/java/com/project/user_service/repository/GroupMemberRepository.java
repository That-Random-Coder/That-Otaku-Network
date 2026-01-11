package com.project.user_service.repository;

import com.project.user_service.domain.dto.response.GetGroups;
import com.project.user_service.domain.dto.response.GroupDetailResponseDto;
import com.project.user_service.domain.entity.groups.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.users.id = :userId")
    Optional<GroupMember> findByGroupIdAndUserId(@Param("groupId") UUID groupId, @Param("userId") UUID userId);

    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.id = :groupId")
    long countByGroupId(@Param("groupId") UUID groupId);

    @Query("""
        SELECT gm.group.id FROM GroupMember gm 
        WHERE gm.users.id IN :userIds
    """)
    Set<UUID> getUserGroups(@Param("userIds") Set<UUID> userIds);

    @Query("SELECT gm.group.id FROM GroupMember gm WHERE gm.users.id = :id OR gm.group.leader.id = :id")
    Set<UUID> getUserGroup(@Param("id") UUID userId);

    @Query("SELECT gm.users.id FROM GroupMember gm WHERE gm.group.id = :groupId OR gm.group.leader.id = :groupId")
    Set<UUID> getAllMemberOfGroup(@Param("groupId") UUID groupID);

    @Query(
            "SELECT new com.project.user_service.domain.dto.response.GetGroups(" +
                    "g.group.id ," +
                    "g.group.groupName ," +
                    "g.group.leader.username ," +
                    "g.group.leader.id" +
                    ")" +
                    "FROM GroupMember g WHERE g.users.id = :id OR g.group.leader.id = :id"
    )
    List<GetGroups> getGroupOfUser(@Param("id") UUID id);


    @Query("SELECT g.group.leader.id FROM GroupMember g WHERE g.users.id = :id")
    Set<UUID> getAllGroupLeaderId(@Param("id") UUID id);

    @Query("SELECT gm.users.id FROM GroupMember gm WHERE gm.group.id IN :ids")
    Set<UUID> getGroupMemberByGroupId(@Param("ids") Set<UUID> ids);
}
