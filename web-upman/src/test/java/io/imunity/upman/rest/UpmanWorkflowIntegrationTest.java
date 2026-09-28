package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.identity.Identity;

class UpmanWorkflowIntegrationTest extends UpmanRESTTestBase
{
	@BeforeEach
	void configureMapper()
	{
		m.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.NON_PRIVATE);
	}

	@Test
	void shouldManageNestedGroupsAndMembersWithRestManagerRole() throws Exception
	{
		createProject("project");
		String base = "/restupm/v1/projects/project";
		HttpPost create = new HttpPost(base + "/groups");
		create.setEntity(new StringEntity("{\"parentPath\":\"\",\"displayedName\":{\"en\":\"Team\"},"
				+ "\"public\":false}", ContentType.APPLICATION_JSON));
		String path = m.readTree(client.execute(host, create, getClientContext(host),
				new BasicHttpClientResponseHandler())).get("path").asText();

		HttpPut update = new HttpPut(base + "/groups");
		update.setEntity(new StringEntity("{\"path\":\"" + path + "\",\"displayedName\":{\"en\":\"Updated Team\"},"
				+ "\"public\":false}", ContentType.APPLICATION_JSON));
		try (ClassicHttpResponse response = client.executeOpen(host, update, getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(204);
		}
		JsonNode group = getJson(base + "/groups/by-path?path=" + path);
		assertThat(group.get("public").asBoolean()).isFalse();
		assertThat(group.get("displayedName").get("en").asText()).isEqualTo("Updated Team");
		HttpPut invalidVisibility = new HttpPut(base + "/groups");
		invalidVisibility.setEntity(new StringEntity("{\"path\":\"" + path
				+ "\",\"displayedName\":{\"en\":\"Rejected name\"},"
				+ "\"public\":true}", ContentType.APPLICATION_JSON));
		try (ClassicHttpResponse response = client.executeOpen(host, invalidVisibility, getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(409);
		}
		assertThat(getJson(base + "/groups/by-path?path=" + path).get("displayedName").get("en").asText())
				.isEqualTo("Updated Team");
		for (String incompleteBody : List.of("{\"path\":\"" + path
				+ "\",\"displayedName\":{\"en\":\"Incomplete\"}}",
				"{\"path\":\"" + path + "\",\"public\":false}",
				"{\"displayedName\":{\"en\":\"Incomplete\"},\"public\":false}"))
		{
			HttpPut incompleteUpdate = new HttpPut(base + "/groups");
			incompleteUpdate.setEntity(new StringEntity(incompleteBody, ContentType.APPLICATION_JSON));
			try (ClassicHttpResponse response = client.executeOpen(host, incompleteUpdate, getClientContext(host)))
			{
				assertThat(response.getCode()).isEqualTo(400);
			}
		}
		assertThat(getJson(base + "/groups/by-path?path=" + path).get("displayedName").get("en").asText())
				.isEqualTo("Updated Team");

		HttpPut addMember = new HttpPut(base + "/members/by-id/" + entityId + "?groupPath=" + path);
		try (ClassicHttpResponse response = client.executeOpen(host, addMember, getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(204);
		}
		JsonNode member = getJson(base + "/members/by-id/" + entityId + "?groupPath=" + path);
		assertThat(member.get("entityId").asLong()).isEqualTo(entityId);
		assertThat(member.get("email").asText()).isEqualTo(entityEmail);
		assertThat(getJson(base + "/members?groupPath=" + path)).hasSize(1);
		HttpPut delegation = new HttpPut(base + "/groups/by-path/delegation?path=" + path);
		delegation.setEntity(new StringEntity("{\"enabled\":true,\"enableSubprojects\":false,"
				+ "\"logoUrl\":null}", ContentType.APPLICATION_JSON));
		try (ClassicHttpResponse response = client.executeOpen(host, delegation, getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(204);
		}
		HttpPut role = new HttpPut(base + "/members/by-id/" + entityId + "/role?groupPath=" + path);
		role.setEntity(new StringEntity("{\"role\":\"manager\"}", ContentType.APPLICATION_JSON));
		try (ClassicHttpResponse response = client.executeOpen(host, role, getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(204);
		}
		assertThat(getJson(base + "/members/by-id/" + entityId + "/role?groupPath=" + path)
				.get("role").asText()).isEqualTo("manager");

		Identity withoutEmail = createUsernameUser("no-email-user", null, DEF_PASSWORD, CRED_REQ_PASS);
		groupsMan.addMemberFromParent("/A", new EntityParam(withoutEmail));
		HttpPut addWithoutEmail = new HttpPut(base + "/members/by-id/" + withoutEmail.getEntityId()
				+ "?groupPath=" + path);
		try (ClassicHttpResponse response = client.executeOpen(host, addWithoutEmail, getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(204);
		}
		assertThat(getJson(base + "/members/by-id/" + withoutEmail.getEntityId() + "?groupPath=" + path)
				.get("email").isNull()).isTrue();
		for (String formPath : List.of("/registrationForm", "/signUpEnquiry", "/membershipUpdateEnquiry"))
		{
			try (ClassicHttpResponse response = client.executeOpen(host,
					new HttpGet(base + formPath + "/link"), getClientContext(host)))
			{
				assertThat(response.getCode()).isEqualTo(404);
			}
		}
		assertThat(getJson(base + "/requests")).isEmpty();
		try (ClassicHttpResponse response = client.executeOpen(host,
				new HttpGet(base + "/requests/foreign"), getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(404);
		}
		try (ClassicHttpResponse response = client.executeOpen(host,
				new HttpGet(base + "/groups/by-path?path=../other"), getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(400);
		}
		assertThat(getJson(base + "/invitations")).isEmpty();
		HttpPost invalidInvitation = new HttpPost(base + "/invitations");
		invalidInvitation.setEntity(new StringEntity("{\"emails\":[\"invalid\"],\"groups\":[],"
				+ "\"expiration\":\"2030-01-01T00:00:00Z\"}", ContentType.APPLICATION_JSON));
		try (ClassicHttpResponse response = client.executeOpen(host, invalidInvitation, getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(400);
		}
		for (String invitationPath : List.of("/invitations/missing", "/invitations/missing/resend",
				"/invitations/missing/reinvite"))
		{
			if (invitationPath.endsWith("/missing"))
			{
				try (ClassicHttpResponse response = client.executeOpen(host,
						new HttpGet(base + invitationPath), getClientContext(host)))
				{
					assertThat(response.getCode()).isEqualTo(404);
				}
			} else
			{
				try (ClassicHttpResponse response = client.executeOpen(host,
						new HttpPost(base + invitationPath), getClientContext(host)))
				{
					assertThat(response.getCode()).isEqualTo(404);
				}
			}
		}
		try (ClassicHttpResponse response = client.executeOpen(host,
				new HttpDelete(base + "/invitations/missing"), getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(404);
		}
		for (String decision : List.of("accept", "decline"))
		{
			try (ClassicHttpResponse response = client.executeOpen(host,
					new HttpPost(base + "/requests/missing/state?decision=" + decision), getClientContext(host)))
			{
				assertThat(response.getCode()).isEqualTo(404);
			}
		}
		for (String query : List.of("", "?decision=unknown"))
		{
			try (ClassicHttpResponse response = client.executeOpen(host,
					new HttpPost(base + "/requests/missing/state" + query), getClientContext(host)))
			{
				assertThat(response.getCode()).isEqualTo(400);
			}
		}
		HttpPut rename = new HttpPut(base + "/groups");
		rename.setEntity(new StringEntity("{\"path\":\"" + path
				+ "\",\"displayedName\":{\"en\":\"Renamed\"},\"public\":false}",
				ContentType.APPLICATION_JSON));
		try (ClassicHttpResponse response = client.executeOpen(host, rename, getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(204);
		}
		assertThat(getJson(base + "/groups/by-path?path=" + path).get("displayedName").get("en").asText())
				.isEqualTo("Renamed");
		try (ClassicHttpResponse response = client.executeOpen(host,
				new HttpDelete(base + "/groups/by-path?path=" + path), getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(204);
		}
	}

	@Test
	void shouldAddressNestedProjectReturnedByListing() throws Exception
	{
		createProject("project");
		String base = "/restupm/v1/projects/project";
		HttpPost create = new HttpPost(base + "/groups");
		create.setEntity(new StringEntity("{\"parentPath\":\"\",\"displayedName\":{\"en\":\"Subproject\"},"
				+ "\"public\":false}", ContentType.APPLICATION_JSON));
		String path = m.readTree(client.execute(host, create, getClientContext(host),
				new BasicHttpClientResponseHandler())).get("path").asText();
		HttpPut delegation = new HttpPut(base + "/groups/by-path/delegation?path=" + path);
		delegation.setEntity(new StringEntity("{\"enabled\":true,\"enableSubprojects\":false,"
				+ "\"logoUrl\":null}", ContentType.APPLICATION_JSON));
		try (ClassicHttpResponse response = client.executeOpen(host, delegation, getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(204);
		}

		JsonNode projects = getJson("/restupm/v1/projects");
		String urlId = null;
		for (JsonNode project : projects)
		{
			if (project.get("projectId").asText().equals("project/" + path))
				urlId = project.get("urlId").asText();
		}
		assertThat(urlId).isNotNull().doesNotContain("/");
		assertThat(getJson("/restupm/v1/projects/" + urlId).get("projectId").asText())
				.isEqualTo("project/" + path);
		assertThat(getJson("/restupm/v1/projects/" + urlId + "/requests")).isEmpty();
	}

	private void createProject(String id) throws Exception
	{
		HttpPost create = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest request = RestProjectCreateRequest.builder()
				.withProjectId(id)
				.withPublic(false)
				.withDisplayedName(Map.of("en", "Project"))
				.withDescription(Map.of("en", "Description"))
				.withEnableSubprojects(true)
				.withReadOnlyAttributes(List.of())
				.build();
		create.setEntity(new StringEntity(m.writeValueAsString(request), ContentType.APPLICATION_JSON));
		try (ClassicHttpResponse response = client.executeOpen(host, create, getClientContext(host)))
		{
			assertThat(response.getCode()).isEqualTo(200);
		}
	}

	private JsonNode getJson(String path) throws Exception
	{
		return m.readTree(client.execute(host, new HttpGet(path), getClientContext(host),
				new BasicHttpClientResponseHandler()));
	}
}
