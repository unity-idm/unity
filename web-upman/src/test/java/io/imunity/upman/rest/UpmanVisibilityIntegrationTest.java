package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;

class UpmanVisibilityIntegrationTest extends UpmanRESTTestBase
{
	@Test
	void shouldReturnJsonConflictWhenMakingProjectPublicUnderPrivateParent() throws Exception
	{
		Group project = createProject();
		HttpPut update = new HttpPut("/restupm/v1/projects/project");
		update.setEntity(new StringEntity("{\"public\":true,\"displayedName\":{\"en\":\"Changed\"},"
				+ "\"description\":{\"en\":\"Changed description\"},\"enableSubprojects\":true,"
				+ "\"readOnlyAttributes\":[]}", ContentType.APPLICATION_JSON));

		try (ClassicHttpResponse response = client.executeOpen(host, update, getClientContext(host)))
		{
			assertPrivateParentConflict(response, "/A");
		}

		Group unchanged = groupsMan.getContents(project.getName(), GroupContents.METADATA).getGroup();
		assertThat(unchanged.isPublic()).isFalse();
		assertThat(unchanged.getDisplayedName()).isEqualTo(project.getDisplayedName());
	}

	@Test
	void shouldReturnJsonConflictWhenMakingSubgroupPublicUnderPrivateProject() throws Exception
	{
		Group project = createProject();
		Group subgroup = new Group(project.getName() + "/team");
		subgroup.setDisplayedName(new I18nString("Team"));
		groupsMan.addGroup(subgroup);
		HttpPut update = new HttpPut("/restupm/v1/projects/project/groups");
		update.setEntity(new StringEntity("{\"path\":\"team\",\"public\":true,"
				+ "\"displayedName\":{\"en\":\"Changed\"}}", ContentType.APPLICATION_JSON));

		try (ClassicHttpResponse response = client.executeOpen(host, update, getClientContext(host)))
		{
			assertPrivateParentConflict(response, "Project");
		}

		Group unchanged = groupsMan.getContents(subgroup.getName(), GroupContents.METADATA).getGroup();
		assertThat(unchanged.isPublic()).isFalse();
		assertThat(unchanged.getDisplayedName()).isEqualTo(subgroup.getDisplayedName());
	}

	private Group createProject() throws Exception
	{
		Group project = new Group("/A/project");
		project.setDisplayedName(new I18nString("Project"));
		project.setDelegationConfiguration(new GroupDelegationConfiguration(true, true, null, null, null,
				null, List.of(), List.of()));
		groupsMan.addGroup(project);
		return project;
	}

	private void assertPrivateParentConflict(ClassicHttpResponse response, String parent) throws Exception
	{
		assertThat(response.getCode()).isEqualTo(409);
		assertThat(response.getEntity()).isNotNull();
		assertThat(response.getEntity().getContentType()).isNotNull();
		assertThat(ContentType.parse(response.getEntity().getContentType()).getMimeType())
				.isEqualTo(ContentType.APPLICATION_JSON.getMimeType());
		JsonNode error = m.readTree(EntityUtils.toString(response.getEntity()));
		assertThat(error.path("error").asText()).isEqualTo("ParentIsPrivateGroupException");
		assertThat(error.path("message").asText()).contains("public", "parent group " + parent + " is private");
	}
}
