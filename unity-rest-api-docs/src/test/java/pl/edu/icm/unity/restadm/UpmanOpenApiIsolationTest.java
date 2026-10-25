package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;

import io.imunity.upman.rest.RESTUpmanController;
import io.imunity.upman.rest.RESTUpmanEndpoint;
import io.imunity.upman.rest.UpmanOpenApi;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;

import jakarta.ws.rs.core.Application;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.rest.RestEndpointHelper;

class UpmanOpenApiIsolationTest
{
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
		var admin = new RESTAdminEndpoint(null, null, null, null, () -> List.of(
				new RESTAdmin(null, null, null, null, null, null, null, null, null, null,
						null, null, null, null, null, null, null, null, null),
				new PolicyDocumentsRESTAdmin(mock(PolicyDocumentManagement.class)),
				new BulkQueryHandler(mock(BulkGroupQueryService.class)),
				new BackupRestoreRESTAdmin(mock(ServerManagement.class), mock(UnityServerConfiguration.class))), null, null);
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
			JsonNode expectedUpman = Json.mapper().readTree(Json.pretty(UpmanOpenApi.export()));
			try (HttpClient client = HttpClient.newHttpClient())
			{
				for (String name : List.of("first", "admin", "second", "admin", "first"))
				{
					var response = client.send(HttpRequest.newBuilder(URI.create(
							"http://localhost:" + port + "/" + name + "/openapi.json")).build(),
							HttpResponse.BodyHandlers.ofString());
					assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
					if (!name.equals("admin"))
						assertThat(Json.mapper().readTree(response.body())).isEqualTo(expectedUpman);
				}
			}
			assertThat(expectedUpman.at("/components/schemas/RestGroup/properties").has("logoUrl")).isTrue();
			assertThat(expectedUpman.at("/components/schemas/RestEntity").isMissingNode()).isTrue();
			assertThat(ModelConverters.getInstance().getConverters()).containsExactlyElementsOf(originalConverters);
		} finally
		{
			server.stop();
			buses.forEach(bus -> bus.shutdown(true));
		}
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
