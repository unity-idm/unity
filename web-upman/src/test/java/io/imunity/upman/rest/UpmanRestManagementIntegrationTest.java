/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.JsonUtil;

import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class UpmanRestManagementIntegrationTest extends UpmanRESTTestBase
{
	private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

	@BeforeEach
	public void setUp()
	{
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.NON_PRIVATE);
	}

	@Test
	public void addedProjectIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest build = RestProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(null)
			.withSignUpEnquiry(new RestSignUpEnquiry(null, true))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, true))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));

		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		RestProjectId projectId = JsonUtil.parse(jsonProjectId, RestProjectId.class);

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id);

		String contents = client.execute(host, getProject, getClientContext(host),
			new BasicHttpClientResponseHandler());
		RestProject project = JsonUtil.parse(contents, RestProject.class);

		assertThat(project.projectId).isEqualTo("A");
		assertThat(project.isPublic).isEqualTo(false);
		assertThat(project.displayedName).isEqualTo(Map.of("en", "superGroup"));
		assertThat(project.description).isEqualTo(Map.of("en", "description"));
		assertThat(project.logoUrl).isEqualTo("/image.png");
		assertThat(project.enableSubprojects).isEqualTo(true);
		assertThat(project.readOnlyAttributes).isEqualTo(List.of());
		assertThat(project.registrationForm).isEqualTo(null);
		assertThat(project.signUpEnquiry).isEqualTo("superGroupJoinEnquiry");
		assertThat(project.membershipUpdateEnquiry).isEqualTo("superGroupUpdateEnquiry");
	}

	@Test
	public void removedProjectIsNotReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest build = RestProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(new RestRegistrationForm(null, false))
			.withSignUpEnquiry(new RestSignUpEnquiry(null, false))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, false))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		RestProjectId projectId = JsonUtil.parse(jsonProjectId, RestProjectId.class);

		HttpDelete removeProject = new HttpDelete("/restupm/v1/projects/" + projectId.id);
		try(ClassicHttpResponse response = client.executeOpen(host, removeProject, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id);
		try(ClassicHttpResponse response = client.executeOpen(host, getProject, getClientContext(host)))
		{
			assertEquals(Status.NOT_FOUND.getStatusCode(), response.getCode());
		}
	}

	@Test
	public void notAddedProjectIsNotReturned() throws Exception
	{
		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + "test");
		try(ClassicHttpResponse response = client.executeOpen(host, getProject, getClientContext(host)))
		{
			assertEquals(Status.NOT_FOUND.getStatusCode(), response.getCode());
		}
	}

	@Test
	public void notAddedProjectIsNotRemoved() throws Exception
	{
		HttpDelete removeProject = new HttpDelete("/restupm/v1/projects/" + "test");
		try(ClassicHttpResponse response = client.executeOpen(host, removeProject, getClientContext(host)))
		{
			assertEquals(Status.NOT_FOUND.getStatusCode(), response.getCode());
		}
	}

	@Test
	public void addedProjectsAreReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest build = RestProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(new RestRegistrationForm(null, true))
			.withSignUpEnquiry(new RestSignUpEnquiry(null, true))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, true))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		RestProjectId projectId = JsonUtil.parse(jsonProjectId, RestProjectId.class);

		HttpPost add2 = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest build2 = RestProjectCreateRequest.builder()
			.withProjectId("B")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(new RestRegistrationForm(null, true))
			.withSignUpEnquiry(new RestSignUpEnquiry(null, true))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, true))
			.build();
		add2.setEntity(new StringEntity(mapper.writeValueAsString(build2)));
		String jsonProjectId2 = client.execute(host, add2, getClientContext(host),
			new BasicHttpClientResponseHandler());
		RestProjectId projectId2 = JsonUtil.parse(jsonProjectId2, RestProjectId.class);

		HttpGet getProject = new HttpGet("/restupm/v1/projects");

		String contents = client.execute(host, getProject, getClientContext(host),
			new BasicHttpClientResponseHandler());
		List<RestProject> projects = JsonUtil.parseToList(contents, RestProject.class);

		assertThat(projects.size()).isEqualTo(2);
		assertThat(projects.stream().map(p -> p.projectId).collect(Collectors.toSet()))
			.isEqualTo(Set.of(projectId.id, projectId2.id));
	}

	@Test
	public void notAddedProjectIsNotUpdate() throws Exception
	{
		HttpPut update = new HttpPut("/restupm/v1/projects/" + "test");
		RestProjectUpdateRequest updateBuild = RestProjectUpdateRequest.builder()
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup2"))
			.withDescription(Map.of("en", "description2"))
			.withEnableDelegation(true)
			.withLogoUrl("/image2.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(new RestRegistrationForm(null, true))
			.withSignUpEnquiry(new RestSignUpEnquiry(null, true))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, true))
			.build();
		update.setEntity(new StringEntity(mapper.writeValueAsString(updateBuild)));

		try(ClassicHttpResponse response = client.executeOpen(host, update, getClientContext(host)))
		{
			assertEquals(Status.NOT_FOUND.getStatusCode(), response.getCode());
		}
	}

	@Test
	public void updatedProjectIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest build = RestProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(new RestRegistrationForm(null, true))
			.withSignUpEnquiry(new RestSignUpEnquiry(null, true))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, true))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		RestProjectId projectId = JsonUtil.parse(jsonProjectId, RestProjectId.class);

		HttpPut update = new HttpPut("/restupm/v1/projects/" + projectId.id);
		RestProjectUpdateRequest updateBuild = RestProjectUpdateRequest.builder()
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup2"))
			.withDescription(Map.of("en", "description2"))
			.withEnableDelegation(true)
			.withLogoUrl("/image2.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(new RestRegistrationForm(null, true))
			.withSignUpEnquiry(null)
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, true))
			.build();
		update.setEntity(new StringEntity(mapper.writeValueAsString(updateBuild)));

		try(ClassicHttpResponse response = client.executeOpen(host, update, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id);

		String contents = client.execute(host, getProject, getClientContext(host),
			new BasicHttpClientResponseHandler());
		RestProject project = JsonUtil.parse(contents, RestProject.class);

		assertThat(project.projectId).isEqualTo("A");
		assertThat(project.isPublic).isEqualTo(false);
		assertThat(project.displayedName).isEqualTo(Map.of("en", "superGroup2"));
		assertThat(project.description).isEqualTo(Map.of("en", "description2"));
		assertThat(project.logoUrl).isEqualTo("/image2.png");
		assertThat(project.enableSubprojects).isEqualTo(true);
		assertThat(project.readOnlyAttributes).isEqualTo(List.of());
		assertThat(project.registrationForm).isEqualTo("superGroupRegistration1");
		assertThat(project.signUpEnquiry).isEqualTo(null);
		assertThat(project.membershipUpdateEnquiry).isEqualTo("superGroupUpdateEnquiry1");
	}

	@Test
	public void addedMembershipIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest build = RestProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(new RestRegistrationForm(null, true))
			.withSignUpEnquiry(new RestSignUpEnquiry(null, true))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, true))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		RestProjectId projectId = JsonUtil.parse(jsonProjectId, RestProjectId.class);

		HttpPost addMembership = new HttpPost("/restupm/v1/projects/" + projectId.id + "/members/" + entityEmail);
		try(ClassicHttpResponse response = client.executeOpen(host, addMembership, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id + "/members/" + entityEmail);
		String contents = client.execute(host, getProject, getClientContext(host),
			new BasicHttpClientResponseHandler());
		RestProjectMembership member = JsonUtil.parse(contents, RestProjectMembership.class);

		assertThat(member.email).isEqualTo(entityEmail);
	}

	@Test
	public void removedMembershipIsNotReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest build = RestProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(new RestRegistrationForm(null, true))
			.withSignUpEnquiry(new RestSignUpEnquiry(null, true))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, true))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		RestProjectId projectId = JsonUtil.parse(jsonProjectId, RestProjectId.class);

		HttpPost addMembership = new HttpPost("/restupm/v1/projects/" + projectId.id + "/members/" + entityEmail);
		try(ClassicHttpResponse response = client.executeOpen(host, addMembership, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpDelete removeMembership = new HttpDelete("/restupm/v1/projects/" + projectId.id + "/members/" + entityEmail);
		try(ClassicHttpResponse response = client.executeOpen(host, removeMembership, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id + "/members/" + entityEmail);
		try(ClassicHttpResponse response = client.executeOpen(host, getProject, getClientContext(host)))
		{
			assertEquals(Status.NOT_FOUND.getStatusCode(), response.getCode());
		}
	}

	@Test
	public void addedMembershipRoleIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest build = RestProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(new RestRegistrationForm(null, true))
			.withSignUpEnquiry(new RestSignUpEnquiry(null, true))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, true))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		RestProjectId projectId = JsonUtil.parse(jsonProjectId, RestProjectId.class);

		HttpPost addMembership = new HttpPost("/restupm/v1/projects/" + projectId.id + "/members/" + entityEmail);
		try(ClassicHttpResponse response = client.executeOpen(host, addMembership, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpPut addMembershipRole = new HttpPut("/restupm/v1/projects/" + projectId.id + "/members/" + entityEmail + "/role");
		addMembershipRole.setEntity(new StringEntity(mapper.writeValueAsString(new RestAuthorizationRole("manager"))));
		try(ClassicHttpResponse response = client.executeOpen(host, addMembershipRole, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id + "/members/" + entityEmail + "/role");
		String contents = client.execute(host, getProject, getClientContext(host),
			new BasicHttpClientResponseHandler());
		RestAuthorizationRole role = JsonUtil.parse(contents, RestAuthorizationRole.class);

		assertThat(role.role).isEqualTo("manager");
	}

	@Test
	public void addedMembershipReturnedDefaultRegularRole() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest build = RestProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(new RestRegistrationForm(null, true))
			.withSignUpEnquiry(new RestSignUpEnquiry(null, true))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, true))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		RestProjectId projectId = JsonUtil.parse(jsonProjectId, RestProjectId.class);

		HttpPost addMembership = new HttpPost("/restupm/v1/projects/" + projectId.id + "/members/" + entityEmail);
		try(ClassicHttpResponse response = client.executeOpen(host, addMembership, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id + "/members/" + entityEmail + "/role");
		String contents = client.execute(host, getProject, getClientContext(host),
			new BasicHttpClientResponseHandler());
		RestAuthorizationRole role = JsonUtil.parse(contents, RestAuthorizationRole.class);

		assertThat(role.role).isEqualTo("regular");
	}

	@Test
	public void updatedMembershipRoleIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest build = RestProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(new RestRegistrationForm(null, true))
			.withSignUpEnquiry(new RestSignUpEnquiry(null, true))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, true))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		RestProjectId projectId = JsonUtil.parse(jsonProjectId, RestProjectId.class);

		HttpPost addMembership = new HttpPost("/restupm/v1/projects/" + projectId.id + "/members/" + entityEmail);
		try(ClassicHttpResponse response = client.executeOpen(host, addMembership, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpPut addMembershipRole = new HttpPut("/restupm/v1/projects/" + projectId.id + "/members/" + entityEmail + "/role");
		addMembershipRole.setEntity(new StringEntity(mapper.writeValueAsString(new RestAuthorizationRole("manager"))));
		try(ClassicHttpResponse response = client.executeOpen(host, addMembershipRole, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpPut addMembershipRoleUpdate = new HttpPut("/restupm/v1/projects/" + projectId.id + "/members/" + entityEmail + "/role");
		addMembershipRoleUpdate.setEntity(new StringEntity(mapper.writeValueAsString(new RestAuthorizationRole("projectsAdmin"))));
		try(ClassicHttpResponse response = client.executeOpen(host, addMembershipRoleUpdate, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id + "/members/" + entityEmail + "/role");
		String contents = client.execute(host, getProject, getClientContext(host),
			new BasicHttpClientResponseHandler());
		RestAuthorizationRole role = JsonUtil.parse(contents, RestAuthorizationRole.class);

		assertThat(role.role).isEqualTo("projectsAdmin");
	}


	@Test
	public void notAddedMemberRoleIsNotReturned() throws Exception
	{
		HttpGet getMemberRole = new HttpGet("/restupm/v1/projects/" + "test" + "/members/" + entityEmail + "/role");
		try(ClassicHttpResponse response = client.executeOpen(host, getMemberRole, getClientContext(host)))
		{
			assertEquals(Status.NOT_FOUND.getStatusCode(), response.getCode());
		}
	}
}
