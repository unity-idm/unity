/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Test;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;

import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class TestUpmanRestManagement extends TestUpmanRESTBase
{
	private final ObjectMapper mapper = Constants.MAPPER;

	@Test
	public void addedProjectIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest build = RestProjectCreateRequest.builder()
			.withGroupName("/A")
			.withIsPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withEnableDelegation(true)
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(null)
			.withRegistrationFormAutogenerate(true)
			.withSignUpEnquiry(null)
			.withSignUpEnquiryAutogenerate(true)
			.withMembershipUpdateEnquiry(null)
			.withMembershipUpdateEnquiryAutogenerate(true)
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));

		try(ClassicHttpResponse response = client.executeOpen(host, add, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getGroupContents = new HttpGet("/restupm/v1/projects/%2FA");

		String contents = client.execute(host, getGroupContents, getClientContext(host), new BasicHttpClientResponseHandler());
		RestProject project = JsonUtil.parse(contents, RestProject.class);

		assertThat(project.groupName).isEqualTo("/A/A");
		assertThat(project.isPublic).isEqualTo(false);
		assertThat(project.displayedName).isEqualTo(Map.of("en", "superGroup"));
		assertThat(project.description).isEqualTo(Map.of("en", "description"));
		assertThat(project.enableDelegation).isEqualTo(true);
		assertThat(project.logoUrl).isEqualTo("/image.png");
		assertThat(project.enableSubprojects).isEqualTo(true);
		assertThat(project.readOnlyAttributes).isEqualTo(List.of());
		assertThat(project.registrationForm).isEqualTo("superGroupRegistration");
		assertThat(project.signUpEnquiry).isEqualTo("superGroupJoinEnquiry");
		assertThat(project.membershipUpdateEnquiry).isEqualTo("superGroupUpdateEnquiry");
	}

	@Test
	public void removedProjectIsNotReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest build = RestProjectCreateRequest.builder()
			.withGroupName("/A")
			.withIsPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withEnableDelegation(false)
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(null)
			.withRegistrationFormAutogenerate(true)
			.withSignUpEnquiry(null)
			.withSignUpEnquiryAutogenerate(true)
			.withMembershipUpdateEnquiry(null)
			.withMembershipUpdateEnquiryAutogenerate(true)
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));

		try(ClassicHttpResponse response = client.executeOpen(host, add, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpDelete removeProject = new HttpDelete("/restupm/v1/projects/%2FA");
		try(ClassicHttpResponse response = client.executeOpen(host, removeProject, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getGroupContents = new HttpGet("/restupm/v1/projects/%2FA");
		try(ClassicHttpResponse response = client.executeOpen(host, getGroupContents, getClientContext(host)))
		{
			assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getCode());
		}
	}


	@Test
	public void addedProjectsAreReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest build = RestProjectCreateRequest.builder()
			.withGroupName("/A")
			.withIsPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withEnableDelegation(true)
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(null)
			.withRegistrationFormAutogenerate(true)
			.withSignUpEnquiry(null)
			.withSignUpEnquiryAutogenerate(true)
			.withMembershipUpdateEnquiry(null)
			.withMembershipUpdateEnquiryAutogenerate(true)
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));

		try(ClassicHttpResponse response = client.executeOpen(host, add, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpPost add2 = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest build2 = RestProjectCreateRequest.builder()
			.withGroupName("/B")
			.withIsPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withEnableDelegation(true)
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(null)
			.withRegistrationFormAutogenerate(true)
			.withSignUpEnquiry(null)
			.withSignUpEnquiryAutogenerate(true)
			.withMembershipUpdateEnquiry(null)
			.withMembershipUpdateEnquiryAutogenerate(true)
			.build();
		add2.setEntity(new StringEntity(mapper.writeValueAsString(build2)));

		try(ClassicHttpResponse response = client.executeOpen(host, add2, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getGroupContents = new HttpGet("/restupm/v1/projects");

		String contents = client.execute(host, getGroupContents, getClientContext(host), new BasicHttpClientResponseHandler());
		List<RestProject> projects = JsonUtil.parseToList(contents, RestProject.class);

		assertThat(projects.size()).isEqualTo(3);
		assertThat(projects.stream().map(p -> p.groupName).collect(Collectors.toSet()))
			.isEqualTo(Set.of("/A", "/A/A", "/A/B"));
	}

	@Test
	public void updatedProjectIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restupm/v1/projects");
		RestProjectCreateRequest build = RestProjectCreateRequest.builder()
			.withGroupName("/A")
			.withIsPublic(false)
			.withDisplayedName(Map.of("en", "superGroup"))
			.withDescription(Map.of("en", "description"))
			.withEnableDelegation(true)
			.withLogoUrl("/image.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(null)
			.withRegistrationFormAutogenerate(true)
			.withSignUpEnquiry(null)
			.withSignUpEnquiryAutogenerate(true)
			.withMembershipUpdateEnquiry(null)
			.withMembershipUpdateEnquiryAutogenerate(true)
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));

		try(ClassicHttpResponse response = client.executeOpen(host, add, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpPut update = new HttpPut("/restupm/v1/projects/%2FA");
		RestProjectUpdateRequest updateBuild = RestProjectUpdateRequest.builder()
			.withIsPublic(false)
			.withDisplayedName(Map.of("en", "superGroup2"))
			.withDescription(Map.of("en", "description2"))
			.withEnableDelegation(true)
			.withLogoUrl("/image2.png")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(null)
			.withRegistrationFormAutogenerate(true)
			.withSignUpEnquiry(null)
			.withSignUpEnquiryAutogenerate(true)
			.withMembershipUpdateEnquiry(null)
			.withMembershipUpdateEnquiryAutogenerate(true)
			.build();
		update.setEntity(new StringEntity(mapper.writeValueAsString(updateBuild)));

		try(ClassicHttpResponse response = client.executeOpen(host, update, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getGroupContents = new HttpGet("/restupm/v1/projects/%2FA");

		String contents = client.execute(host, getGroupContents, getClientContext(host), new BasicHttpClientResponseHandler());
		RestProject project = JsonUtil.parse(contents, RestProject.class);

		assertThat(project.groupName).isEqualTo("/A/A");
		assertThat(project.isPublic).isEqualTo(false);
		assertThat(project.displayedName).isEqualTo(Map.of("en", "superGroup2"));
		assertThat(project.description).isEqualTo(Map.of("en", "description2"));
		assertThat(project.enableDelegation).isEqualTo(true);
		assertThat(project.logoUrl).isEqualTo("/image2.png");
		assertThat(project.enableSubprojects).isEqualTo(true);
		assertThat(project.readOnlyAttributes).isEqualTo(List.of());
		assertThat(project.registrationForm).isEqualTo("superGroupRegistration1");
		assertThat(project.signUpEnquiry).isEqualTo("superGroupJoinEnquiry1");
		assertThat(project.membershipUpdateEnquiry).isEqualTo("superGroupUpdateEnquiry1");
	}

	@Test
	public void addedMembershipIsReturned() throws Exception
	{
		HttpPost addMembership = new HttpPost("/restupm/v1/projects/%2F/members/" + entityId);
		try(ClassicHttpResponse response = client.executeOpen(host, addMembership, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getGroupContents = new HttpGet("/restupm/v1/projects/%2F/members/" + entityId);
		String contents = client.execute(host, getGroupContents, getClientContext(host), new BasicHttpClientResponseHandler());
		RestProjectMembership member = JsonUtil.parse(contents, RestProjectMembership.class);

		assertThat(member.group).isEqualTo("/A");
		assertThat(member.entityId).isEqualTo(entityId);
	}

	@Test
	public void removedMembershipIsNotReturned() throws Exception
	{
		HttpPost addMembership = new HttpPost("/restupm/v1/projects/%2F/members/" + entityId);
		try(ClassicHttpResponse response = client.executeOpen(host, addMembership, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpDelete removeMembership = new HttpDelete("/restupm/v1/projects/%2F/members/" + entityId);
		try(ClassicHttpResponse response = client.executeOpen(host, removeMembership, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getGroupContents = new HttpGet("/restupm/v1/projects/%2F/members/" + entityId);
		try(ClassicHttpResponse response = client.executeOpen(host, getGroupContents, getClientContext(host)))
		{
			assertEquals(Status.FORBIDDEN.getStatusCode(), response.getCode());
		}
	}

	@Test
	public void addedMembershipRoleIsReturned() throws Exception
	{
		HttpPut addMembershipRole = new HttpPut("/restupm/v1/projects/%2F/members/" + entityId + "/role");
		addMembershipRole.setEntity(new StringEntity(mapper.writeValueAsString(new RestAuthorizationRole("manager"))));
		try(ClassicHttpResponse response = client.executeOpen(host, addMembershipRole, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getGroupContents = new HttpGet("/restupm/v1/projects/%2F/members/" + entityId + "/role");
		String contents = client.execute(host, getGroupContents, getClientContext(host), new BasicHttpClientResponseHandler());
		RestAuthorizationRole role = JsonUtil.parse(contents, RestAuthorizationRole.class);

		assertThat(role.role).isEqualTo("manager");
	}

	@Test
	public void updatedMembershipRoleIsReturned() throws Exception
	{
		HttpPut addMembershipRole = new HttpPut("/restupm/v1/projects/%2F/members/" + entityId + "/role");
		addMembershipRole.setEntity(new StringEntity(mapper.writeValueAsString(new RestAuthorizationRole("manager"))));
		try(ClassicHttpResponse response = client.executeOpen(host, addMembershipRole, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpPut addMembershipRoleUpdate = new HttpPut("/restupm/v1/projects/%2F/members/" + entityId + "/role");
		addMembershipRoleUpdate.setEntity(new StringEntity(mapper.writeValueAsString(new RestAuthorizationRole("projectsAdmin"))));
		try(ClassicHttpResponse response = client.executeOpen(host, addMembershipRoleUpdate, getClientContext(host)))
		{
			assertEquals(Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getGroupContents = new HttpGet("/restupm/v1/projects/%2F/members/" + entityId + "/role");
		String contents = client.execute(host, getGroupContents, getClientContext(host), new BasicHttpClientResponseHandler());
		RestAuthorizationRole role = JsonUtil.parse(contents, RestAuthorizationRole.class);

		assertThat(role.role).isEqualTo("projectsAdmin");
	}

}
