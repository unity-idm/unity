package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.BadRequestException;

class ProjectPathProviderTest
{
	@Test
	void shouldResolveDirectProjectId()
	{
		assertThat(ProjectPathProvider.getProjectPath("project", "/root")).isEqualTo("/root/project");
		assertThat(ProjectPathProvider.getProjectPath("project", "/")).isEqualTo("/project");
		assertThat(ProjectPathProvider.urlId("project")).isEqualTo("project");
		assertThat(ProjectPathProvider.getProjectPath("~n~YS9i", "/root"))
				.isEqualTo("/root/~n~YS9i");
	}

	@Test
	void shouldResolveListedNestedProjectIdWithoutUrlSlashes()
	{
		String urlId = ProjectPathProvider.urlId("parent/nested");

		assertThat(urlId).isEqualTo("parent%2Fnested");
		assertThat(ProjectPathProvider.getProjectPath(URLDecoder.decode(urlId, StandardCharsets.UTF_8), "/root"))
				.isEqualTo("/root/parent/nested");
	}

	@Test
	void shouldKeepDirectProjectIdsDistinctFromNestedProjectIds()
	{
		String nestedUrlId = ProjectPathProvider.urlId("a/b");
		String prefixedDirectUrlId = ProjectPathProvider.urlId("~n~YS9i");
		String percentDirectUrlId = ProjectPathProvider.urlId("a%2Fb");

		assertThat(nestedUrlId).isEqualTo("a%2Fb");
		assertThat(prefixedDirectUrlId).isEqualTo("~n~YS9i");
		assertThat(percentDirectUrlId).startsWith("%2F~e~").doesNotContain("%25");
		assertThat(ProjectPathProvider.getProjectPath(
				URLDecoder.decode(prefixedDirectUrlId, StandardCharsets.UTF_8), "/root"))
				.isEqualTo("/root/~n~YS9i");
		assertThat(ProjectPathProvider.getProjectPath(
				URLDecoder.decode(percentDirectUrlId, StandardCharsets.UTF_8), "/root"))
				.isEqualTo("/root/a%2Fb");
	}

	@Test
	void shouldKeepDirectNamesLookingLikeEscapedIdsLiteral()
	{
		String directId = "~e~YSUyRmI";
		String urlId = ProjectPathProvider.urlId(directId);

		assertThat(urlId).isEqualTo(directId);
		assertThat(urlId).isNotEqualTo(ProjectPathProvider.urlId("a%2Fb"));
		assertThat(ProjectPathProvider.getProjectPath(
				URLDecoder.decode(urlId, StandardCharsets.UTF_8), "/root"))
				.isEqualTo("/root/" + directId);
		assertThat(ProjectPathProvider.getNewProjectPath(directId, "/root")).isEqualTo("/root/" + directId);
	}

	@Test
	void shouldResolveDecodedPercentSignsInProjectAndGroupNames()
	{
		String projectUrlId = ProjectPathProvider.urlId("team%west");
		String projectId = URLDecoder.decode(projectUrlId, StandardCharsets.UTF_8);
		String groupPath = URLDecoder.decode("group%252Fname", StandardCharsets.UTF_8);

		assertThat(projectUrlId).startsWith("%2F~e~").doesNotContain("%25");
		assertThat(ProjectPathProvider.getProjectPath(projectId, "/root")).isEqualTo("/root/team%west");
		assertThat(ProjectPathProvider.resolveGroupPath("/root/team%west", groupPath))
				.isEqualTo("/root/team%west/group%2Fname");
	}

	@Test
	void shouldEncodeSpacesAndPlusSignsForPathSegments()
	{
		String urlId = ProjectPathProvider.urlId("team +west");

		assertThat(urlId).isEqualTo("team%20%2Bwest");
		assertThat(ProjectPathProvider.getProjectPath(URLDecoder.decode(urlId, StandardCharsets.UTF_8), "/root"))
				.isEqualTo("/root/team +west");
	}

	@Test
	void shouldRejectTraversalAndExternalPaths()
	{
		for (String path : new String[] {"../other", "one/../other", "/other", "one//other"})
		{
			assertThatThrownBy(() -> ProjectPathProvider.resolveGroupPath("/root/project", path))
					.isInstanceOf(BadRequestException.class);
		}
		String decodedTraversal = URLDecoder.decode("one/%2e%2e", StandardCharsets.UTF_8);
		assertThatThrownBy(() -> ProjectPathProvider.resolveGroupPath("/root/project", decodedTraversal))
				.isInstanceOf(BadRequestException.class);
	}
}
