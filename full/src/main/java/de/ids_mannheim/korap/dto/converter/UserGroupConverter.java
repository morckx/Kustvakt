package de.ids_mannheim.korap.dto.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import de.ids_mannheim.korap.constant.GroupMemberStatus;
import de.ids_mannheim.korap.dto.UserGroupDto;
import de.ids_mannheim.korap.dto.UserGroupMemberDto;
import de.ids_mannheim.korap.entity.Role;
import de.ids_mannheim.korap.entity.UserGroup;
import de.ids_mannheim.korap.entity.UserGroupMember;

@Component
public class UserGroupConverter {

    public UserGroupDto createUserGroupDto (UserGroup group,
            List<UserGroupMember> members, GroupMemberStatus userMemberStatus,
            List<Role> userRoles) {

        UserGroupDto dto = new UserGroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setOwner(group.getCreatedBy());
        dto.setUserMemberStatus(userMemberStatus);

        List<String> roles = new ArrayList<>(userRoles.size());
        for (Role r : userRoles) {
            roles.add(r.getName());
        }
        dto.setUserRoles(roles);

        if (members != null) {
            ArrayList<UserGroupMemberDto> memberDtos =
                    new ArrayList<>(members.size());
            for (UserGroupMember member : members) {

                UserGroupMemberDto memberDto = new UserGroupMemberDto();
                memberDto.setUserId(member.getUserId());
                memberDto.setStatus(member.getStatus());
                List<String> memberRoles = new ArrayList<>(member.getRoles().size());
                for (Role r : member.getRoles()) {
                    memberRoles.add(r.getName());
                }
                memberDto.setRoles(memberRoles);
                memberDtos.add(memberDto);
            }
            dto.setMembers(memberDtos);
        }
        else {
            dto.setMembers(new ArrayList<UserGroupMemberDto>());
        }

        return dto;
    }

}
