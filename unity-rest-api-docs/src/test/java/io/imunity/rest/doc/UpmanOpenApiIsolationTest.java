package io.imunity.rest.doc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;

import io.imunity.upman.rest.RESTUpmanController;
import io.imunity.upman.rest.RESTUpmanEndpoint;
import io.imunity.upman.rest.UpmanOpenApi;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;

import jakarta.ws.rs.core.Application;

import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.rest.RESTEndpoint;
import pl.edu.icm.unity.rest.RESTEndpointProperties;
import pl.edu.icm.unity.rest.RestEndpointHelper;
import pl.edu.icm.unity.rest.jwt.JWTAuthenticationProperties;
import pl.edu.icm.unity.rest.web.console.JWTServiceConfiguration;
import pl.edu.icm.unity.restadm.BackupRestoreRESTAdmin;
import pl.edu.icm.unity.restadm.BulkQueryHandler;
import pl.edu.icm.unity.restadm.PolicyDocumentsRESTAdmin;
import pl.edu.icm.unity.restadm.RESTAdmin;
import pl.edu.icm.unity.restadm.RESTAdminEndpoint;
import pl.edu.icm.unity.restadm.web.console.RestAdminServiceConfiguration;

class UpmanOpenApiIsolationTest
{
	private AnnotationConfigApplicationContext adminContext;

	@AfterEach
	void closeAdminContext()
	{
		if (adminContext != null)
			adminContext.close();
	}

	@Test
	void shouldKeepUpmanIsolatedBesideTheExistingAdminApplication() throws Exception
	{
		var originalConverters = List.copyOf(ModelConverters.getInstance().getConverters());
		var factory = mock(RESTUpmanController.RESTUpmanControllerFactory.class);
		when(factory.newInstance(anyString(), anyString(), anyList())).thenReturn(new RESTUpmanController());
		var first = new RESTUpmanEndpoint(null, null, null, null, factory, null, null);
		first.initialize(null, List.of(),
				"unity.upman.rest.rootGroup=/first\nunity.upman.rest.authorizationGroup=/first-auth\n");
		var second = new RESTUpmanEndpoint(null, null, null, null, factory, null, null);
		second.initialize(null, List.of(),
				"unity.upman.rest.rootGroup=/second\nunity.upman.rest.authorizationGroup=/second-auth\n");
		var admin = adminEndpoint();
		List<Bus> buses = new ArrayList<>();
		Server server = new Server(0);
		try
		{
			var contexts = new ContextHandlerCollection();
			contexts.addHandler(context("/first", first.new UpmanRESTJAXRSApp(), buses));
			contexts.addHandler(context("/second", second.new UpmanRESTJAXRSApp(), buses));
			contexts.addHandler(context("/admin", admin.new RESTAdminJAXRSApp(), buses));
			server.setHandler(contexts);
			server.start();
			int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
			try (HttpClient client = HttpClient.newHttpClient())
			{
				JsonNode expectedAdmin = readOpenApi(client, port, "admin");
				assertThat(expectedAdmin.path("paths").has("/v1/entity/{entityId}")).isTrue();
				assertThat(expectedAdmin.path("paths").has("/v1/policy-documents")).isTrue();
				assertThat(expectedAdmin.path("paths").has("/v1/projects")).isFalse();
				assertThat(expectedAdmin.at("/components/schemas").has("Project")).isFalse();
				assertThat(expectedAdmin.at("/paths/~1v1~1entity~1{entityId}/get/responses")
						.findValuesAsText("type")).contains("string");
				JsonNode expectedUpman = Json.mapper().readTree(Json.pretty(UpmanOpenApi.export()));
				for (String name : List.of("first", "admin", "second", "admin", "first"))
				{
					assertThat(readOpenApi(client, port, name)).as(name + " OpenAPI")
							.isEqualTo(name.equals("admin") ? expectedAdmin : expectedUpman);
				}
				assertThat(expectedUpman.at("/components/schemas/ProjectGroup/properties").has("logoUrl")).isTrue();
				assertThat(expectedUpman.at("/components/schemas/RestEntity").isMissingNode()).isTrue();
			}
			assertThat(ModelConverters.getInstance().getConverters()).containsExactlyElementsOf(originalConverters);
		} finally
		{
			server.stop();
			buses.forEach(bus -> bus.shutdown(true));
		}
	}

	@ParameterizedTest(name = "runtime documentation setting: {0}")
	@MethodSource("publicationSettings")
	void shouldControlPublicationIndependentlyForAdminAndUpman(PublicationCase scenario,
			@TempDir Path output) throws Exception
	{
		List<Bus> buses = new ArrayList<>();
		Server server = new Server(0);
		try
		{
			var contexts = new ContextHandlerCollection();
			contexts.addHandler(configuredContext("/upman", new UpmanEndpoint(), scenario.setting()));
			contexts.addHandler(configuredContext("/admin", new AdminEndpoint(adminEndpoint()), scenario.setting()));
			String opposite = Boolean.toString(!scenario.enabled());
			contexts.addHandler(configuredContext("/other-upman", new UpmanEndpoint(), opposite));
			contexts.addHandler(configuredContext("/other-admin", new AdminEndpoint(adminEndpoint()), opposite));
			server.setHandler(contexts);
			server.start();
			for (var handler : contexts.getHandlers())
			{
				var context = (ServletContextHandler) handler;
				buses.add(((CXFNonSpringServlet) context.getServletHandler().getServlets()[0].getServlet()).getBus());
			}
			int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
			try (HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build())
			{
				assertPublication(client, port, "upman", scenario.enabled());
				assertPublication(client, port, "admin", scenario.enabled());
				assertPublication(client, port, "other-upman", !scenario.enabled());
				assertPublication(client, port, "other-admin", !scenario.enabled());
				for (String application : List.of("upman", "other-upman", "admin", "other-admin"))
				{
					String route = application.contains("upman") ? "/v1/projects" : "/v1/policy-documents";
					var response = get(client, port, application + route);
					assertThat(response.statusCode()).as(application + " API: " + response.body()).isEqualTo(200);
					assertThat(Json.mapper().readTree(response.body()).isArray()).isTrue();
				}
				if (!scenario.enabled())
				{
					OpenApiExporter.main(output.toString());
					assertThat(Files.readString(output.resolve("upman-openapi-v1.yaml")))
							.contains("/v1/projects:", "Unity Project Management API");
					assertThat(Files.readString(output.resolve("upman-openapi-v1.html"))).contains("Redoc.init");
					assertPublication(client, port, "upman", false);
					assertPublication(client, port, "admin", false);
				}
			}
		} finally
		{
			server.stop();
			buses.forEach(bus -> bus.shutdown(true));
		}
	}

	@ParameterizedTest(name = "console configuration setting: {0}")
	@MethodSource("publicationSettings")
	void shouldPreserveSharedPublicationSettingWhenSavingAdminAndJwt(PublicationCase scenario) throws Exception
	{
		String configuration = publicationProperty(scenario.setting());
		var admin = new RestAdminServiceConfiguration();
		admin.fromProperties(configuration, null);
		var jwt = new JWTServiceConfiguration();
		jwt.fromProperties(configuration + JWTAuthenticationProperties.PREFIX
				+ JWTAuthenticationProperties.SIGNING_CREDENTIAL + "=credential\n", null);

		for (String serialized : List.of(admin.toProperties(), jwt.toProperties()))
		{
			Properties raw = new Properties();
			raw.load(new StringReader(serialized));
			assertThat(new RESTEndpointProperties(raw).getBooleanValue(RESTEndpointProperties.ENABLE_OPEN_API))
					.isEqualTo(scenario.enabled());
		}
	}

	private ServletContextHandler configuredContext(String path, RESTEndpoint endpoint, String setting)
	{
		var description = new ResolvedEndpoint(new Endpoint(path, "test", path, null, 0), null, null);
		endpoint.initialize(description, List.of(), "unity.upman.rest.rootGroup=/projects\n"
				+ "unity.upman.rest.authorizationGroup=/managers\n" + publicationProperty(setting));
		return endpoint.getServletContextHandler();
	}

	private static String publicationProperty(String setting)
	{
		return setting == null ? "" : RESTEndpointProperties.PREFIX + RESTEndpointProperties.ENABLE_OPEN_API
				+ "=" + setting + "\n";
	}

	private void assertPublication(HttpClient client, int port, String application, boolean enabled) throws Exception
	{
		for (String resource : List.of("openapi.json", "openapi.yaml", "api-docs", "api-docs/",
				"api-docs/index.html", "api-docs/scalar.js"))
		{
			var response = get(client, port, application + "/" + resource);
			assertThat(response.statusCode()).as(application + "/" + resource)
					.isEqualTo(enabled ? 200 : 404);
		}
		for (String obsolete : List.of("api-docs/swagger-ui.css", "swagger-ui.css", "swagger-ui-bundle.js"))
			assertThat(get(client, port, application + "/" + obsolete).statusCode()).as(obsolete).isEqualTo(404);
	}

	private HttpResponse<String> get(HttpClient client, int port, String path) throws Exception
	{
		return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/" + path)).build(),
				HttpResponse.BodyHandlers.ofString());
	}

	private static Stream<PublicationCase> publicationSettings()
	{
		return Stream.of(
				PublicationCase.builder().enabled(true).build(),
				PublicationCase.builder().setting("true").enabled(true).build(),
				PublicationCase.builder().setting("false").enabled(false).build());
	}

	private record PublicationCase(String setting, boolean enabled)
	{
		static Builder builder()
		{
			return new Builder();
		}

		@Override
		public String toString()
		{
			return setting == null ? "omitted" : setting;
		}

		private static class Builder
		{
			private String setting;
			private boolean enabled;

			Builder setting(String setting)
			{
				this.setting = setting;
				return this;
			}

			Builder enabled(boolean enabled)
			{
				this.enabled = enabled;
				return this;
			}

			PublicationCase build()
			{
				return new PublicationCase(setting, enabled);
			}
		}
	}

	private static class UpmanEndpoint extends RESTUpmanEndpoint
	{
		UpmanEndpoint() throws Exception
		{
			super(null, null, null, null, upmanFactory(), null, null);
		}

		private static RESTUpmanController.RESTUpmanControllerFactory upmanFactory() throws Exception
		{
			var controller = spy(new RESTUpmanController());
			doReturn(List.of()).when(controller).getProjects();
			var factory = mock(RESTUpmanController.RESTUpmanControllerFactory.class);
			when(factory.newInstance(anyString(), anyString(), anyList())).thenReturn(controller);
			return factory;
		}

		@Override
		protected void addInterceptors(List<Interceptor<? extends Message>> in,
				List<Interceptor<? extends Message>> out)
		{
		}
	}

	private static class AdminEndpoint extends RESTEndpoint
	{
		private final RESTAdminEndpoint admin;

		AdminEndpoint(RESTAdminEndpoint admin)
		{
			super(null, null, null, null, null, "", null);
			this.admin = admin;
		}

		@Override
		protected Application getApplication()
		{
			return admin.new RESTAdminJAXRSApp();
		}

		@Override
		protected void addInterceptors(List<Interceptor<? extends Message>> in,
				List<Interceptor<? extends Message>> out)
		{
		}
	}

	private RESTAdminEndpoint adminEndpoint()
	{
		if (adminContext == null)
		{
			adminContext = new AnnotationConfigApplicationContext();
			Map<Class<?>, Object> dependencies = Map.of(
					MessageSource.class, mock(MessageSource.class),
					SessionManagement.class, mock(SessionManagement.class),
					NetworkServer.class, mock(NetworkServer.class),
					AuthenticationProcessor.class, mock(AuthenticationProcessor.class),
					AdvertisedAddressProvider.class, mock(AdvertisedAddressProvider.class),
					EntityManagement.class, mock(EntityManagement.class),
					PolicyDocumentManagement.class, mock(PolicyDocumentManagement.class),
					BulkGroupQueryService.class, mock(BulkGroupQueryService.class),
					ServerManagement.class, mock(ServerManagement.class),
					UnityServerConfiguration.class, mock(UnityServerConfiguration.class));
			dependencies.forEach((type, bean) -> adminContext.getBeanFactory().registerSingleton(type.getName(), bean));
			adminContext.registerBean(RESTAdmin.class, () -> new RESTAdmin(null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null));
			adminContext.register(PolicyDocumentsRESTAdmin.class, BulkQueryHandler.class, BackupRestoreRESTAdmin.class,
					RESTAdminEndpoint.class);
			adminContext.refresh();
		}
		return adminContext.getBean(RESTAdminEndpoint.class);
	}

	private JsonNode readOpenApi(HttpClient client, int port, String application) throws Exception
	{
		var response = client.send(HttpRequest.newBuilder(URI.create(
				"http://localhost:" + port + "/" + application + "/openapi.json")).build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		return Json.mapper().readTree(response.body());
	}

	private ServletContextHandler context(String path, Application application, List<Bus> buses)
	{
		Bus bus = BusFactory.newInstance().createBus();
		buses.add(bus);
		var servlet = new CXFNonSpringServlet();
		servlet.setBus(bus);
		var context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		context.setContextPath(path);
		context.addServlet(new ServletHolder(servlet), "/*");
		RestEndpointHelper.createCxfEndpoint(application, bus);
		return context;
	}
}
