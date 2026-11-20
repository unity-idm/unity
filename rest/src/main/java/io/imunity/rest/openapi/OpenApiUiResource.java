package io.imunity.rest.openapi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

@Hidden
@Path("/api-docs")
public class OpenApiUiResource
{
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String index(@Context UriInfo uriInfo)
	{
		String base = uriInfo.getBaseUri().getRawPath().replace("&", "&amp;").replace("\"", "&quot;");
		return """
				<!doctype html>
				<html lang="en"><head><meta charset="utf-8">
				<meta name="viewport" content="width=device-width, initial-scale=1">
				<base href="%s"><title>Unity API reference</title></head>
				<body><div id="unity-api-reference"></div>
				<script src="api-docs/scalar.js"></script>
				<script>Scalar.createApiReference('#unity-api-reference', {
				url: new URL('openapi.json', document.baseURI).href,
				servers: [{url: document.baseURI.replace(/\\/$/, '')}],
				withDefaultFonts: false, proxyUrl: '', persistAuth: false,
				showDeveloperTools: 'never', hideClientButton: true,
				agent: {disabled: true}, mcp: {disabled: true}
				});</script></body></html>
				""".formatted(base);
	}

	@GET
	@Path("index.html")
	@Produces(MediaType.TEXT_HTML)
	public String indexFile(@Context UriInfo uriInfo)
	{
		return index(uriInfo);
	}

	@GET
	@Path("scalar.js")
	@Produces("text/javascript")
	public InputStream script() throws IOException
	{
		Properties artifact = new Properties();
		try (var input = resource("/META-INF/maven/org.webjars.npm/scalar__api-reference/pom.properties"))
		{
			artifact.load(input);
		}
		return resource("/META-INF/resources/webjars/scalar__api-reference/" + artifact.getProperty("version")
				+ "/dist/browser/standalone.js");
	}

	private InputStream resource(String path)
	{
		return Objects.requireNonNull(OpenApiUiResource.class.getResourceAsStream(path), "Missing renderer " + path);
	}
}
