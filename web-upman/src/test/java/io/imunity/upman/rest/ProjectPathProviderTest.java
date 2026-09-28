package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
		assertThat(ProjectPathProvider.getProjectPath("~n~direct", "/root"))
				.isEqualTo("/root/~n~direct");
	}

	@Test
	void shouldResolveListedNestedProjectIdWithoutUrlSlashes()
	{
		String urlId = ProjectPathProvider.urlId("parent/nested");

		assertThat(urlId).doesNotContain("/");
		assertThat(ProjectPathProvider.getProjectPath(urlId, "/root")).isEqualTo("/root/parent/nested");
	}

	@Test
	void shouldRejectTraversalAndExternalPaths()
	{
		for (String path : new String[] {"../other", "one/../other", "/other", "one//other", "one/%2e%2e"})
		{
			assertThatThrownBy(() -> ProjectPathProvider.resolveGroupPath("/root/project", path))
					.isInstanceOf(BadRequestException.class);
		}
	}
}
