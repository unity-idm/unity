package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;

class UpmanMemberRemovalIntegrationTest extends UpmanRESTTestBase
{
	private static final String PROJECT = "/A/project";
	private static final String BASE = "/restupm/v1/projects/project";

	@ParameterizedTest
	@MethodSource("removalCases")
	void shouldPreserveSoleManagerAfterRejectedDemotionAndDeletion(RemovalCase testCase) throws Exception
	{
		createProject(testCase.role());
		addOtherMember(GroupAuthorizationRole.regular);
		String memberPath = memberPath(testCase);
		HttpPut demote = new HttpPut(memberPath + "/role");
		demote.setEntity(new StringEntity("{\"role\":\"regular\"}", ContentType.APPLICATION_JSON));

		try (ClassicHttpResponse response = client.executeOpen(host, demote, getClientContext(host)))
		{
			assertManagerConflict(response);
		}
		try (ClassicHttpResponse response = client.executeOpen(host, new HttpDelete(memberPath), getClientContext(host)))
		{
			assertManagerConflict(response);
		}

		assertThat(member(entityId).path("role").asText()).isEqualTo(testCase.role().name());
		assertThat(members()).hasSize(2);
	}

	@ParameterizedTest
	@MethodSource("removalCases")
	void shouldAllowRemovingManagerWhenAnotherManagerRemains(RemovalCase testCase) throws Exception
	{
		createProject(testCase.role());
		Identity remaining = addOtherMember(GroupAuthorizationRole.manager);

		try (ClassicHttpResponse response = client.executeOpen(host,
				new HttpDelete(memberPath(testCase)), getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(204);
		}

		assertThat(members()).hasSize(1);
		assertThat(member(remaining.getEntityId()).path("role").asText()).isEqualTo("manager");
	}

	@ParameterizedTest
	@MethodSource("removalCases")
	void shouldAllowRemovingRegularMemberWhilePreservingManager(RemovalCase testCase) throws Exception
	{
		createProject(GroupAuthorizationRole.regular);
		Identity remaining = addOtherMember(testCase.role());

		try (ClassicHttpResponse response = client.executeOpen(host,
				new HttpDelete(memberPath(testCase)), getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(204);
		}

		assertThat(members()).hasSize(1);
		assertThat(member(remaining.getEntityId()).path("role").asText()).isEqualTo(testCase.role().name());
	}

	@Test
	void shouldAllowRemovingProjectManagerFromOrdinarySubgroup() throws Exception
	{
		createProject(GroupAuthorizationRole.manager);
		groupsMan.addGroup(new Group(PROJECT + "/team"));
		groupsMan.addMemberFromParent(PROJECT + "/team", new EntityParam(entityId));

		try (ClassicHttpResponse response = client.executeOpen(host,
				new HttpDelete(BASE + "/members/by-id/" + entityId + "?groupPath=team"), getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(204);
		}

		assertThat(idsMan.getGroups(new EntityParam(entityId))).containsKey(PROJECT).doesNotContainKey(PROJECT + "/team");
		assertThat(member(entityId).path("role").asText()).isEqualTo("manager");
	}

	private void createProject(GroupAuthorizationRole role) throws Exception
	{
		Group project = new Group(PROJECT);
		project.setDelegationConfiguration(new GroupDelegationConfiguration(true, true, null, null, null,
				null, List.of(), List.of()));
		groupsMan.addGroup(project);
		addProjectMember(entityId, role);
	}

	private Identity addOtherMember(GroupAuthorizationRole role) throws Exception
	{
		Identity identity = createUsernameUser("other-member", null, DEF_PASSWORD, CRED_REQ_PASS);
		groupsMan.addMemberFromParent("/A", new EntityParam(identity));
		addProjectMember(identity.getEntityId(), role);
		return identity;
	}

	private void addProjectMember(long memberId, GroupAuthorizationRole role) throws Exception
	{
		groupsMan.addMemberFromParent(PROJECT, new EntityParam(memberId));
		attrsMan.createAttribute(new EntityParam(memberId),
				new Attribute("sys:ProjectManagementRole", "enumeration", PROJECT, List.of(role.name())));
	}

	private String memberPath(RemovalCase testCase)
	{
		return BASE + "/members/" + (testCase.byEmail() ? entityEmail : "by-id/" + entityId);
	}

	private JsonNode member(long memberId) throws Exception
	{
		return getJson(BASE + "/members/by-id/" + memberId);
	}

	private JsonNode members() throws Exception
	{
		return getJson(BASE + "/members?groupPath=");
	}

	private JsonNode getJson(String path) throws Exception
	{
		return m.readTree(client.execute(host, new HttpGet(path), getClientContext(host),
				new BasicHttpClientResponseHandler()));
	}

	private void assertManagerConflict(ClassicHttpResponse response) throws Exception
	{
		assertThat(response.getCode()).isEqualTo(409);
		assertThat(ContentType.parse(response.getEntity().getContentType()).getMimeType())
				.isEqualTo(ContentType.APPLICATION_JSON.getMimeType());
		JsonNode error = m.readTree(EntityUtils.toString(response.getEntity()));
		assertThat(error.path("error").asText()).isEqualTo("OneManagerRemainsException");
		assertThat(error.path("message").asText()).contains("At least one manager", PROJECT);
	}

	private static Stream<RemovalCase> removalCases()
	{
		return Stream.of(
				RemovalCase.builder().withRole(GroupAuthorizationRole.manager).build(),
				RemovalCase.builder().withRole(GroupAuthorizationRole.projectsAdmin).build(),
				RemovalCase.builder().withRole(GroupAuthorizationRole.manager).byEmail().build(),
				RemovalCase.builder().withRole(GroupAuthorizationRole.projectsAdmin).byEmail().build());
	}

	private record RemovalCase(boolean byEmail, GroupAuthorizationRole role)
	{
		static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private boolean byEmail;
			private GroupAuthorizationRole role;

			Builder byEmail()
			{
				byEmail = true;
				return this;
			}

			Builder withRole(GroupAuthorizationRole role)
			{
				this.role = role;
				return this;
			}

			RemovalCase build()
			{
				return new RemovalCase(byEmail, role);
			}
		}
	}
}
