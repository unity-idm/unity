package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.BulkProcessingManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.UserImportManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.event.EventPublisherWithAuthz;
import pl.edu.icm.unity.engine.api.groupMember.GroupMembersService;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.token.SecuredTokensManagement;
import pl.edu.icm.unity.engine.api.translation.ExternalDataParser;
import pl.edu.icm.unity.rest.RestEndpointHelper;
import pl.edu.icm.unity.restadm.token.Token2JsonFormatter;

abstract class AdminTransportHttpTestBase
{
	protected final EntityManagement identitiesMan = mock(EntityManagement.class);
	protected final GroupsManagement groupsMan = mock(GroupsManagement.class);
	protected final AttributesManagementRESTService attributesService = mock(AttributesManagementRESTService.class);
	protected final EmailConfirmationManager confirmationManager = mock(EmailConfirmationManager.class);
	protected final EndpointManagement endpointManagement = mock(EndpointManagement.class);
	protected final RegistrationsManagement registrationManagement = mock(RegistrationsManagement.class);
	protected final BulkProcessingManagement bulkProcessingManagement = mock(BulkProcessingManagement.class);
	protected final UserImportManagement userImportManagement = mock(UserImportManagement.class);
	protected final EntityCredentialManagement entityCredMan = mock(EntityCredentialManagement.class);
	protected final AttributeTypeManagement attributeTypeMan = mock(AttributeTypeManagement.class);
	protected final InvitationManagement invitationMan = mock(InvitationManagement.class);
	protected final EventPublisherWithAuthz eventPublisher = mock(EventPublisherWithAuthz.class);
	protected final SecuredTokensManagement securedTokenMan = mock(SecuredTokensManagement.class);
	protected final Token2JsonFormatter jsonFormatter = mock(Token2JsonFormatter.class);
	protected final UserNotificationTriggerer userNotificationTriggerer = mock(UserNotificationTriggerer.class);
	protected final ExternalDataParser dataParser = mock(ExternalDataParser.class);
	protected final IdpStatisticManagement idpStatisticManagement = mock(IdpStatisticManagement.class);
	protected final GroupMembersService groupMembersService = mock(GroupMembersService.class);
	protected final EnquiryManagement enquiryManagement = mock(EnquiryManagement.class);
	protected final BulkGroupQueryService bulkQueryService = mock(BulkGroupQueryService.class);
	protected final ServerManagement serverManagement = mock(ServerManagement.class);
	protected final UnityServerConfiguration serverConfig = mock(UnityServerConfiguration.class);
	protected final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
	private Server server;
	private Bus bus;
	private URI base;
	private final HttpClient client = HttpClient.newBuilder().build();

	@BeforeEach
	void startTransport() throws Exception
	{
		var handler = new RESTAdmin(identitiesMan, groupsMan, attributesService, confirmationManager,
				endpointManagement, registrationManagement, bulkProcessingManagement, userImportManagement,
				entityCredMan, attributeTypeMan, invitationMan, eventPublisher, securedTokenMan, jsonFormatter,
				userNotificationTriggerer, dataParser, idpStatisticManagement, groupMembersService, enquiryManagement);
		var endpoint = new RESTAdminEndpoint(null, null, null, null,
				() -> List.of(handler, new BulkQueryHandler(bulkQueryService),
						new PolicyDocumentsRESTAdmin(mock(PolicyDocumentManagement.class)),
						new BackupRestoreRESTAdmin(serverManagement, serverConfig)), null, identitiesMan);
		bus = BusFactory.newInstance().createBus();
		var servlet = new CXFNonSpringServlet();
		servlet.setBus(bus);
		var context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		context.setContextPath("/");
		context.getServletHandler().setDecodeAmbiguousURIs(true);
		context.addServlet(new ServletHolder(servlet), "/*");
		RestEndpointHelper.createCxfEndpoint(endpoint.new RESTAdminJAXRSApp(), bus);
		server = new Server();
		var configuration = new org.eclipse.jetty.server.HttpConfiguration();
		configuration.setUriCompliance(new org.eclipse.jetty.http.UriCompliance("allowForEncodedPathSeparators",
				java.util.Set.of(org.eclipse.jetty.http.UriCompliance.Violation.AMBIGUOUS_PATH_SEPARATOR)));
		var connector = new ServerConnector(server, new org.eclipse.jetty.server.HttpConnectionFactory(configuration));
		connector.setPort(0);
		server.addConnector(connector);
		server.setHandler(context);
		server.start();
		base = URI.create("http://localhost:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort());
	}

	@AfterEach
	void stopTransport() throws Exception
	{
		if (server != null)
			server.stop();
		if (bus != null)
			bus.shutdown(true);
	}

	protected HttpResponse<String> exchange(String method, String path, String body, String contentType) throws Exception
	{
		var request = HttpRequest.newBuilder(base.resolve(path)).method(method, body == null
				? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
		if (contentType != null)
			request.header("Content-Type", contentType);
		return client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	protected String fixture(String name) throws Exception
	{
		try (var input = java.util.Objects.requireNonNull(getClass()
				.getResourceAsStream("/io/imunity/rest/admin/contract/" + name)))
		{
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	protected void assertJson(String path, String expected) throws Exception
	{
		var response = exchange("GET", path, null, null);
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(response.headers().firstValue("content-type").orElse("")).startsWith("application/json");
		assertThat(json.readTree(response.body())).isEqualTo(json.readTree(expected));
	}
}
