/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.Response.Status;
import pl.edu.icm.unity.base.json.JsonUtil;

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
		ProjectCreateRequest build = ProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));

		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		ProjectId projectId = JsonUtil.parse(jsonProjectId, ProjectId.class);

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id());

		String contents = client.execute(host, getProject, getClientContext(host),
			new BasicHttpClientResponseHandler());
		Project project = JsonUtil.parse(contents, Project.class);

		assertThat(project.projectId()).isEqualTo("A");
		assertThat(project.isPublic()).isEqualTo(false);
		assertThat(project.displayedName()).isEqualTo(Map.of("en", "superGroup"));
		assertThat(project.description()).isEqualTo(Map.of("en", "description"));
		assertThat(project.logoUrl()).isEqualTo("/image.png");
		assertThat(project.enableSubprojects()).isEqualTo(true);
		assertThat(project.readOnlyAttributes()).isEqualTo(List.of());
		assertThat(project.registrationForm()).isEqualTo(null);
		assertThat(project.signUpEnquiry()).isEqualTo(null);
		assertThat(project.membershipUpdateEnquiry()).isEqualTo(null);
	}

	@Test
	public void removedProjectIsNotReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		ProjectCreateRequest build = ProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		ProjectId projectId = JsonUtil.parse(jsonProjectId, ProjectId.class);

		HttpDelete removeProject = new HttpDelete("/restupm/v1/projects/" + projectId.id());
		try(ClassicHttpResponse response = client.executeOpen(host, removeProject, getClientContext(host)))
		{
			assertThat(Status.NO_CONTENT.getStatusCode()).isEqualTo(response.getCode());
		}

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id());
		try(ClassicHttpResponse response = client.executeOpen(host, getProject, getClientContext(host)))
		{
			assertThat(Status.NOT_FOUND.getStatusCode()).isEqualTo(response.getCode());
		}
	}

	@Test
	public void notAddedProjectIsNotReturned() throws Exception
	{
		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + "test");
		try(ClassicHttpResponse response = client.executeOpen(host, getProject, getClientContext(host)))
		{
			assertThat(Status.NOT_FOUND.getStatusCode()).isEqualTo(response.getCode());
		}
	}

	@Test
	public void notAddedProjectIsNotRemoved() throws Exception
	{
		HttpDelete removeProject = new HttpDelete("/restupm/v1/projects/" + "test");
		try(ClassicHttpResponse response = client.executeOpen(host, removeProject, getClientContext(host)))
		{
			assertThat(Status.NOT_FOUND.getStatusCode()).isEqualTo(response.getCode());
		}
	}

	@Test
	public void addedProjectsAreReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		ProjectCreateRequest build = ProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		ProjectId projectId = JsonUtil.parse(jsonProjectId, ProjectId.class);

		HttpPost add2 = new HttpPost("/restupm/v1/projects");
		ProjectCreateRequest build2 = ProjectCreateRequest.builder()
			.withProjectId("B")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			
			.build();
		add2.setEntity(new StringEntity(mapper.writeValueAsString(build2)));
		String jsonProjectId2 = client.execute(host, add2, getClientContext(host),
			new BasicHttpClientResponseHandler());
		ProjectId projectId2 = JsonUtil.parse(jsonProjectId2, ProjectId.class);

		HttpGet getProject = new HttpGet("/restupm/v1/projects");

		String contents = client.execute(host, getProject, getClientContext(host),
			new BasicHttpClientResponseHandler());
		List<Project> projects = JsonUtil.parseToList(contents, Project.class);

		assertThat(projects.size()).isEqualTo(2);
		assertThat(projects.stream().map(p -> p.projectId()).collect(Collectors.toSet()))
			.isEqualTo(Set.of(projectId.id(), projectId2.id()));
	}

	@Test
	public void notAddedProjectIsNotUpdate() throws Exception
	{
		HttpPut update = new HttpPut("/restupm/v1/projects/" + "test");
		ProjectUpdateRequest updateBuild = ProjectUpdateRequest.builder()
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup2"))
			.withDescription(Map.of("en", "description2"))
			.withEnableDelegation(true)
			.withLogoUrl("/image2.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			
			.build();
		update.setEntity(new StringEntity(mapper.writeValueAsString(updateBuild)));

		try(ClassicHttpResponse response = client.executeOpen(host, update, getClientContext(host)))
		{
			assertThat(Status.NOT_FOUND.getStatusCode()).isEqualTo(response.getCode());
		}
	}

	@Test
	public void updatedProjectIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		ProjectCreateRequest build = ProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		ProjectId projectId = JsonUtil.parse(jsonProjectId, ProjectId.class);

		HttpPut update = new HttpPut("/restupm/v1/projects/" + projectId.id());
		ProjectUpdateRequest updateBuild = ProjectUpdateRequest.builder()
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup2"))
			.withDescription(Map.of("en", "description2"))
			.withEnableDelegation(true)
			.withLogoUrl("/image2.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.build();
		update.setEntity(new StringEntity(mapper.writeValueAsString(updateBuild)));

		try(ClassicHttpResponse response = client.executeOpen(host, update, getClientContext(host)))
		{
			assertThat(Status.NO_CONTENT.getStatusCode()).isEqualTo(response.getCode());
		}

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id());

		String contents = client.execute(host, getProject, getClientContext(host),
			new BasicHttpClientResponseHandler());
		Project project = JsonUtil.parse(contents, Project.class);

		assertThat(project.projectId()).isEqualTo("A");
		assertThat(project.isPublic()).isEqualTo(false);
		assertThat(project.displayedName()).isEqualTo(Map.of("en", "superGroup2"));
		assertThat(project.description()).isEqualTo(Map.of("en", "description2"));
		assertThat(project.logoUrl()).isEqualTo("/image2.png");
		assertThat(project.enableSubprojects()).isEqualTo(true);
		assertThat(project.readOnlyAttributes()).isEqualTo(List.of());
		assertThat(project.registrationForm()).isEqualTo(null);
		assertThat(project.signUpEnquiry()).isEqualTo(null);
		assertThat(project.membershipUpdateEnquiry()).isEqualTo(null);
	}

	@Test
	public void addedMembershipIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		ProjectCreateRequest build = ProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		ProjectId projectId = JsonUtil.parse(jsonProjectId, ProjectId.class);

		HttpPost addMembership = new HttpPost("/restupm/v1/projects/" + projectId.id() + "/members/" + entityEmail);
		try(ClassicHttpResponse response = client.executeOpen(host, addMembership, getClientContext(host)))
		{
			assertThat(Status.NO_CONTENT.getStatusCode()).isEqualTo(response.getCode());
		}

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id() + "/members/" + entityEmail);
		String contents = client.execute(host, getProject, getClientContext(host),
			new BasicHttpClientResponseHandler());
		ProjectMembership member = JsonUtil.parse(contents, ProjectMembership.class);

		assertThat(member.email()).isEqualTo(entityEmail);
	}

	@Test
	public void removedMembershipIsNotReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		ProjectCreateRequest build = ProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		ProjectId projectId = JsonUtil.parse(jsonProjectId, ProjectId.class);

		HttpPost addMembership = new HttpPost("/restupm/v1/projects/" + projectId.id() + "/members/" + entityEmail);
		try(ClassicHttpResponse response = client.executeOpen(host, addMembership, getClientContext(host)))
		{
			assertThat(Status.NO_CONTENT.getStatusCode()).isEqualTo(response.getCode());
		}

		HttpDelete removeMembership = new HttpDelete("/restupm/v1/projects/" + projectId.id() + "/members/" + entityEmail);
		try(ClassicHttpResponse response = client.executeOpen(host, removeMembership, getClientContext(host)))
		{
			assertThat(Status.NO_CONTENT.getStatusCode()).isEqualTo(response.getCode());
		}

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id() + "/members/" + entityEmail);
		try(ClassicHttpResponse response = client.executeOpen(host, getProject, getClientContext(host)))
		{
			assertThat(Status.NOT_FOUND.getStatusCode()).isEqualTo(response.getCode());
		}
	}

	@Test
	public void addedMembershipRoleIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		ProjectCreateRequest build = ProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		ProjectId projectId = JsonUtil.parse(jsonProjectId, ProjectId.class);

		HttpPost addMembership = new HttpPost("/restupm/v1/projects/" + projectId.id() + "/members/" + entityEmail);
		try(ClassicHttpResponse response = client.executeOpen(host, addMembership, getClientContext(host)))
		{
			assertThat(Status.NO_CONTENT.getStatusCode()).isEqualTo(response.getCode());
		}

		HttpPut addMembershipRole = new HttpPut(
				"/restupm/v1/projects/" + projectId.id() + "/members/" + entityEmail + "/role");
		addMembershipRole.setEntity(new StringEntity(mapper.writeValueAsString(new AuthorizationRole("manager"))));
		try(ClassicHttpResponse response = client.executeOpen(host, addMembershipRole, getClientContext(host)))
		{
			assertThat(Status.NO_CONTENT.getStatusCode()).isEqualTo(response.getCode());
		}

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id() + "/members/" + entityEmail + "/role");
		String contents = client.execute(host, getProject, getClientContext(host),
			new BasicHttpClientResponseHandler());
		AuthorizationRole role = JsonUtil.parse(contents, AuthorizationRole.class);

		assertThat(role.role()).isEqualTo("manager");
	}

	@Test
	public void addedMembershipReturnedDefaultRegularRole() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		ProjectCreateRequest build = ProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		ProjectId projectId = JsonUtil.parse(jsonProjectId, ProjectId.class);

		HttpPost addMembership = new HttpPost("/restupm/v1/projects/" + projectId.id() + "/members/" + entityEmail);
		try(ClassicHttpResponse response = client.executeOpen(host, addMembership, getClientContext(host)))
		{
			assertThat(Status.NO_CONTENT.getStatusCode()).isEqualTo(response.getCode());
		}

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id() + "/members/" + entityEmail + "/role");
		String contents = client.execute(host, getProject, getClientContext(host),
			new BasicHttpClientResponseHandler());
		AuthorizationRole role = JsonUtil.parse(contents, AuthorizationRole.class);

		assertThat(role.role()).isEqualTo("regular");
	}

	@Test
	public void updatedMembershipRoleIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		ProjectCreateRequest build = ProjectCreateRequest.builder()
			.withProjectId("A")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		String jsonProjectId = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		ProjectId projectId = JsonUtil.parse(jsonProjectId, ProjectId.class);

		HttpPost addMembership = new HttpPost("/restupm/v1/projects/" + projectId.id() + "/members/" + entityEmail);
		try(ClassicHttpResponse response = client.executeOpen(host, addMembership, getClientContext(host)))
		{
			assertThat(Status.NO_CONTENT.getStatusCode()).isEqualTo(response.getCode());
		}

		HttpPut addMembershipRole = new HttpPut(
				"/restupm/v1/projects/" + projectId.id() + "/members/" + entityEmail + "/role");
		addMembershipRole.setEntity(new StringEntity(mapper.writeValueAsString(new AuthorizationRole("manager"))));
		try(ClassicHttpResponse response = client.executeOpen(host, addMembershipRole, getClientContext(host)))
		{
			assertThat(Status.NO_CONTENT.getStatusCode()).isEqualTo(response.getCode());
		}

		HttpPut addMembershipRoleUpdate = new HttpPut(
				"/restupm/v1/projects/" + projectId.id() + "/members/" + entityEmail + "/role");
		addMembershipRoleUpdate.setEntity(
				new StringEntity(mapper.writeValueAsString(new AuthorizationRole("projectsAdmin"))));
		try(ClassicHttpResponse response = client.executeOpen(host, addMembershipRoleUpdate, getClientContext(host)))
		{
			assertThat(Status.NO_CONTENT.getStatusCode()).isEqualTo(response.getCode());
		}

		HttpGet getProject = new HttpGet("/restupm/v1/projects/" + projectId.id() + "/members/" + entityEmail + "/role");
		String contents = client.execute(host, getProject, getClientContext(host),
			new BasicHttpClientResponseHandler());
		AuthorizationRole role = JsonUtil.parse(contents, AuthorizationRole.class);

		assertThat(role.role()).isEqualTo("projectsAdmin");
	}


	@Test
	public void notAddedMemberRoleIsNotReturned() throws Exception
	{
		HttpGet getMemberRole = new HttpGet("/restupm/v1/projects/" + "test" + "/members/" + entityEmail + "/role");
		try(ClassicHttpResponse response = client.executeOpen(host, getMemberRole, getClientContext(host)))
		{
			assertThat(Status.NOT_FOUND.getStatusCode()).isEqualTo(response.getCode());
		}
	}
}
