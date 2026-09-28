package io.imunity.upman.rest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

record RestInvitationRequest(List<String> emails, List<String> groups, boolean allowModifyGroups, Instant expiration)
{
}

record RestInvitationOutcome(String email, String status, String message)
{
}

record RestInvitation(String code, String email, List<String> groups, boolean allowModifyGroups,
		Instant expiration, Instant lastSentTime, int numberOfSends, String link)
{
}

record RestGroupCreateRequest(String parentPath, Map<String, String> displayedName,
		@JsonProperty("public") boolean isPublic)
{
}

record RestGroupUpdateRequest(String path, Map<String, String> displayedName, @JsonProperty("public") Boolean isPublic)
{
}

record RestGroupDelegationRequest(boolean enabled, boolean enableSubprojects, String logoUrl)
{
}

record RestGroupPath(String path)
{
}

record RestGroup(String path, Map<String, String> displayedName, @JsonProperty("public") boolean isPublic,
		boolean delegated, boolean enableSubprojects, String logoUrl, List<String> subGroups)
{
}

record RestGroupMember(long entityId, String name, String email, String role, List<RestAttribute> attributes)
{
}

record RestRequest(String id, String operation, String type, String name, String email,
		Boolean emailVerified, List<String> groups, Instant requestedTime)
{
}

record RestFormLink(String link)
{
}
