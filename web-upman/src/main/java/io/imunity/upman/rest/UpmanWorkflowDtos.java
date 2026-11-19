package io.imunity.upman.rest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

record InvitationRequest(List<String> emails, List<String> groups, boolean allowModifyGroups, Instant expiration)
{
}

record InvitationOutcome(String email, String status, String message)
{
}

record Invitation(String code, String email, List<String> groups, boolean allowModifyGroups,
		Instant expiration, Instant lastSentTime, int numberOfSends, String link)
{
}

record GroupCreateRequest(String parentPath, Map<String, String> displayedName,
		@JsonProperty("public") boolean isPublic)
{
}

record GroupUpdateRequest(String path, Map<String, String> displayedName, @JsonProperty("public") Boolean isPublic)
{
}

record GroupDelegationRequest(boolean enabled, boolean enableSubprojects, String logoUrl)
{
}

record GroupPath(String path)
{
}

record ProjectGroup(String path, Map<String, String> displayedName, @JsonProperty("public") boolean isPublic,
		boolean delegated, boolean enableSubprojects, String logoUrl, List<String> subGroups)
{
}

record GroupMember(long entityId, String name, String email, String role, List<MemberAttribute> attributes)
		implements Member
{
}

record Request(String id, String operation, String type, String name, String email,
		Boolean emailVerified, List<String> groups, Instant requestedTime)
{
}

record FormLink(String link)
{
}
