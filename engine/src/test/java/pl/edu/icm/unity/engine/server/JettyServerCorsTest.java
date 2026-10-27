package pl.edu.icm.unity.engine.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.edu.icm.unity.engine.api.config.UnityHttpServerConfiguration;
import pl.edu.icm.unity.engine.api.utils.URLFactory;

class JettyServerCorsTest
{
	private static final String TRUSTED_ORIGIN = "https://trusted.example";
	private static final String UNTRUSTED_ORIGIN = "https://untrusted.example";
	private JettyServer server;

	@AfterEach
	void stopServer()
	{
		if (server != null)
			server.stop();
	}

	@ParameterizedTest
	@MethodSource("originCases")
	void shouldHonorConfiguredOriginsAfterStartup(OriginCase scenario) throws Exception
	{
		startServer(Map.of("cors.allowedOrigins", scenario.allowedOrigins(), "cors.allowCredentials", "false"), false);

		HttpResponse<String> response = request("GET", scenario.requestOrigin(), Map.of());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).isEqualTo("endpoint response");
		assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
				.isEqualTo(scenario.allowed() ? java.util.Optional.of(scenario.requestOrigin())
						: java.util.Optional.empty());
		assertThat(response.headers().firstValue("Access-Control-Allow-Credentials")).isEmpty();
	}

	private static Stream<OriginCase> originCases()
	{
		return Stream.of(
				OriginCase.builder().requestOrigin(TRUSTED_ORIGIN).allowed().build(),
				OriginCase.builder().requestOrigin(UNTRUSTED_ORIGIN).build(),
				OriginCase.builder().requestOrigin("https://trustedXexample").build(),
				OriginCase.builder().requestOrigin("https://trusted.example.attacker.test").build(),
				OriginCase.builder().requestOrigin("http://trusted.example").build(),
				OriginCase.builder().requestOrigin("https://trusted.example:8443").build(),
				OriginCase.builder().requestOrigin("null").build(),
				OriginCase.builder().allowedOrigins("https://*.trusted.example")
						.requestOrigin("https://app.trusted.example").allowed().build(),
				OriginCase.builder().allowedOrigins("https://*.trusted.example")
						.requestOrigin("https://appXtrusted.example").build(),
				OriginCase.builder().allowedOrigins("https://*.trusted.example")
						.requestOrigin("https://app.trusted.example.attacker.test").build(),
				OriginCase.builder().allowedOrigins("https://[::1]:8443")
						.requestOrigin("https://[::1]:8443").allowed().build(),
				OriginCase.builder().allowedOrigins("https://other.example, " + TRUSTED_ORIGIN)
						.requestOrigin(TRUSTED_ORIGIN).allowed().build(),
				OriginCase.builder().allowedOrigins(TRUSTED_ORIGIN + ", " + TRUSTED_ORIGIN)
						.requestOrigin(TRUSTED_ORIGIN).allowed().build(),
				OriginCase.builder().allowedOrigins("").requestOrigin(TRUSTED_ORIGIN).build(),
				OriginCase.builder().allowedOrigins("*").requestOrigin(UNTRUSTED_ORIGIN).allowed().build());
	}

	@Test
	void shouldHonorPolicyForDynamicallyDeployedEndpoint() throws Exception
	{
		startServer(Map.of("cors.allowedOrigins", TRUSTED_ORIGIN, "cors.allowCredentials", "false"), true);

		HttpResponse<String> trusted = request("GET", TRUSTED_ORIGIN, Map.of());
		HttpResponse<String> untrusted = request("GET", UNTRUSTED_ORIGIN, Map.of());

		assertThat(trusted.headers().firstValue("Access-Control-Allow-Origin")).contains(TRUSTED_ORIGIN);
		assertThat(trusted.headers().firstValue("Access-Control-Allow-Credentials")).isEmpty();
		assertThat(untrusted.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
		assertThat(untrusted.headers().firstValue("Access-Control-Allow-Credentials")).isEmpty();
	}

	@Test
	void shouldAllowCredentialsOnlyForTrustedOrigin() throws Exception
	{
		startServer(Map.of("cors.allowedOrigins", TRUSTED_ORIGIN, "cors.allowCredentials", "true"), false);

		HttpResponse<String> trusted = request("GET", TRUSTED_ORIGIN, Map.of());
		HttpResponse<String> untrusted = request("GET", UNTRUSTED_ORIGIN, Map.of());

		assertThat(trusted.headers().firstValue("Access-Control-Allow-Origin")).contains(TRUSTED_ORIGIN);
		assertThat(trusted.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");
		assertThat(untrusted.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
		assertThat(untrusted.headers().firstValue("Access-Control-Allow-Credentials")).isEmpty();
	}

	@Test
	void shouldHonorPreflightSettings() throws Exception
	{
		startServer(Map.of("cors.allowedOrigins", TRUSTED_ORIGIN, "cors.allowCredentials", "false",
				"cors.allowedMethods", "GET,PATCH", "cors.allowedHeaders", "X-Client",
				"cors.preflightMaxAge", "123", "cors.exposedHeaders", "X-Result"), false);

		Map<String, String> preflightHeaders = Map.of("Access-Control-Request-Method", "PATCH",
				"Access-Control-Request-Headers", "X-Client");
		HttpResponse<String> trusted = request("OPTIONS", TRUSTED_ORIGIN, preflightHeaders);
		HttpResponse<String> untrusted = request("OPTIONS", UNTRUSTED_ORIGIN, preflightHeaders);
		HttpResponse<String> actual = request("GET", TRUSTED_ORIGIN, Map.of());

		assertThat(trusted.statusCode()).isEqualTo(200);
		assertThat(trusted.headers().firstValue("Access-Control-Allow-Origin")).contains(TRUSTED_ORIGIN);
		assertThat(trusted.headers().firstValue("Access-Control-Allow-Credentials")).isEmpty();
		assertThat(trusted.headers().firstValue("Access-Control-Allow-Methods").orElseThrow().split(","))
				.containsExactlyInAnyOrder("GET", "PATCH");
		assertThat(trusted.headers().firstValue("Access-Control-Allow-Headers")).contains("X-Client");
		assertThat(trusted.headers().firstValue("Access-Control-Max-Age")).contains("123");
		assertThat(trusted.headers().firstValue("X-Reached-Servlet")).isEmpty();
		assertThat(untrusted.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
		assertThat(untrusted.headers().firstValue("Access-Control-Allow-Credentials")).isEmpty();
		assertThat(actual.headers().firstValue("Access-Control-Expose-Headers")).contains("X-Result");
	}

	@Test
	void shouldChainPreflightWhenConfigured() throws Exception
	{
		startServer(Map.of("cors.allowedOrigins", TRUSTED_ORIGIN, "cors.chainPreflight", "true"), false);

		HttpResponse<String> response = request("OPTIONS", TRUSTED_ORIGIN,
				Map.of("Access-Control-Request-Method", "GET"));

		assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains(TRUSTED_ORIGIN);
		assertThat(response.headers().firstValue("X-Reached-Servlet")).contains("true");
	}

	@Test
	void shouldLeaveRequestsWithoutOriginUnaffected() throws Exception
	{
		startServer(Map.of("cors.allowedOrigins", TRUSTED_ORIGIN), false);

		HttpResponse<String> response = request("GET", null, Map.of());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).isEqualTo("endpoint response");
		assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
		assertThat(response.headers().firstValue("Access-Control-Allow-Credentials")).isEmpty();
	}

	@Test
	void shouldNotAddCorsHeadersWhenDisabled() throws Exception
	{
		startServer(Map.of("enableCORS", "false"), false);

		HttpResponse<String> response = request("GET", UNTRUSTED_ORIGIN, Map.of());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
		assertThat(response.headers().firstValue("Access-Control-Allow-Credentials")).isEmpty();
	}

	private void startServer(Map<String, String> settings, boolean deployAfterStartup) throws Exception
	{
		Properties properties = new Properties();
		properties.setProperty(UnityHttpServerConfiguration.PREFIX + "port", "0");
		properties.setProperty(UnityHttpServerConfiguration.PREFIX + "host", "127.0.0.1");
		properties.setProperty(UnityHttpServerConfiguration.PREFIX + "enableCORS", "true");
		settings.forEach((key, value) -> properties.setProperty(UnityHttpServerConfiguration.PREFIX + key, value));
		server = new JettyServer(new UnityHttpServerConfiguration(properties), "webContents", null, null,
				new URL[] {URLFactory.of("http://127.0.0.1:0")});
		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/test");
		context.addServlet(new ServletHolder(new EndpointServlet()), "/*");
		if (deployAfterStartup)
			server.start();
		server.deployHandler(context, "sys:cors-test");
		if (!deployAfterStartup)
			server.start();
		assertThat(server.isRunning()).isTrue();
	}

	private HttpResponse<String> request(String method, String origin, Map<String, String> headers) throws Exception
	{
		URI uri = URI.create("http://127.0.0.1:" + server.getUrls()[0].getPort() + "/test/resource");
		HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5))
				.method(method, HttpRequest.BodyPublishers.noBody());
		if (origin != null)
			request.header("Origin", origin);
		headers.forEach(request::header);
		try (HttpClient client = HttpClient.newHttpClient())
		{
			return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
		}
	}

	private static class EndpointServlet extends HttpServlet
	{
		@Override
		protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException
		{
			response.setHeader("X-Reached-Servlet", "true");
			response.getWriter().write("endpoint response");
		}
	}

	private record OriginCase(String allowedOrigins, String requestOrigin, boolean allowed)
	{
		static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private String allowedOrigins = TRUSTED_ORIGIN;
			private String requestOrigin;
			private boolean allowed;

			Builder allowedOrigins(String allowedOrigins)
			{
				this.allowedOrigins = allowedOrigins;
				return this;
			}

			Builder requestOrigin(String requestOrigin)
			{
				this.requestOrigin = requestOrigin;
				return this;
			}

			Builder allowed()
			{
				allowed = true;
				return this;
			}

			OriginCase build()
			{
				return new OriginCase(allowedOrigins, requestOrigin, allowed);
			}
		}
	}
}
