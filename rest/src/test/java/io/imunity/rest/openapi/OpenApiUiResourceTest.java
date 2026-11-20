package io.imunity.rest.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Test;

class OpenApiUiResourceTest
{
	@Test
	void shouldUseTheCurrentEndpointForTheSpecificationAssetsAndRequests()
	{
		var uriInfo = mock(UriInfo.class);
		when(uriInfo.getBaseUri()).thenReturn(URI.create("https://unity.example/custom/deployment/rest/"));
		var resource = new OpenApiUiResource();

		String html = resource.index(uriInfo);

		assertThat(html).contains("<base href=\"/custom/deployment/rest/\">", "src=\"api-docs/scalar.js\"",
				"Scalar.createApiReference('#unity-api-reference'", "id=\"unity-api-reference\"", "new URL('openapi.json', document.baseURI).href",
				"document.baseURI.replace(/\\/$/, '')", "withDefaultFonts: false", "proxyUrl: ''", "persistAuth: false",
				"hideClientButton: true", "agent: {disabled: true}", "mcp: {disabled: true}")
				.doesNotContain("https://", "SwaggerUI", "id=\"api-reference\"");
		assertThat(resource.indexFile(uriInfo)).isEqualTo(html);
	}

	@Test
	void shouldServeTheBundledStandaloneRenderer() throws Exception
	{
		var resource = new OpenApiUiResource();

		try (var input = resource.script())
		{
			String script = new String(input.readAllBytes(), StandardCharsets.UTF_8);

			assertThat(script).hasSizeGreaterThan(100_000).contains("createApiReference");
		}
	}
}
