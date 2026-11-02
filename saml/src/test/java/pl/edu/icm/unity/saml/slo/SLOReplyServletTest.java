package pl.edu.icm.unity.saml.slo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.eclipse.jetty.http.HttpTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.samly2.binding.HttpRedirectBindingSupport;
import eu.unicore.samly2.binding.SAMLMessageType;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.engine.api.session.SessionParticipantTypesRegistry;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.FreemarkerXHTMLHandler;
import pl.edu.icm.unity.saml.SAMLSessionParticipant;
import pl.edu.icm.unity.saml.ServletTestServer;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;

class SLOReplyServletTest
{
	private static final String CONSUMER_PATH = "/unitygw/SAMLSLOResponseConsumer";
	private static final String CONSUMER_ADDRESS = "https://unity.example" + CONSUMER_PATH;
	private KeyPair signingKeys;
	private String logoutResponse;
	private ServletTestServer server;

	@BeforeEach
	void setUp() throws Exception
	{
		signingKeys = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		logoutResponse = """
				<samlp:LogoutResponse xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
				 xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion" ID="_response" Version="2.0"
				 IssueInstant="%s" Destination="%s" InResponseTo="_logoutRequest">
				 <saml:Issuer>https://peer.example</saml:Issuer>
				 <samlp:Status><samlp:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/></samlp:Status>
				</samlp:LogoutResponse>
				""".formatted(Instant.now(), CONSUMER_ADDRESS);
		UnityServerConfiguration configuration = mock(UnityServerConfiguration.class);
		when(configuration.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH))
				.thenReturn("../vaadin-styles/src/main/resources");
		SLOAsyncMessageHandler responseHandler = new SLOAsyncMessageHandler(
				new FreemarkerAppHandler(configuration), new FreemarkerXHTMLHandler(configuration));
		X509Certificate certificate = mock(X509Certificate.class);
		when(certificate.getPublicKey()).thenReturn(signingKeys.getPublic());
		PKIManagement pkiManagement = mock(PKIManagement.class);
		when(pkiManagement.getCertificate("peer-cert")).thenReturn(new NamedCertificate("peer-cert", certificate));
		LogoutContextsStore contexts = new LogoutContextsStore();
		contexts.addInternalContext("logout-state", pendingLogout());
		InternalLogoutProcessor processor = new InternalLogoutProcessor(pkiManagement, contexts,
				responseHandler, CONSUMER_ADDRESS);
		server = new ServletTestServer(new SLOReplyServlet(processor), CONSUMER_PATH);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		server.close();
	}

	@Test
	void shouldCompleteLogoutWithSignedRedirectResponse() throws Exception
	{
		String redirectUrl = signedRedirectUrl();

		HttpTester.Response response = receiveResponse(redirectUrl);

		assertThat(response.getStatus()).isEqualTo(303);
		assertThat(response.get("Location")).isEqualTo("/logged-out");
	}

	@Test
	void shouldRejectUnsignedRedirectLogoutResponseAndAllowSignedRetry() throws Exception
	{
		String redirectUrl = HttpRedirectBindingSupport.getRedirectURL(SAMLMessageType.SAMLResponse,
				"logout-state", logoutResponse, CONSUMER_ADDRESS);

		HttpTester.Response response = receiveResponse(redirectUrl);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.get("Content-Type")).startsWith("text/html");
		assertThat(response.getContent()).contains("An invalid logout response");
		assertThat(response.get("Location")).isNull();
		HttpTester.Response retry = receiveResponse(signedRedirectUrl());
		assertThat(retry.getStatus()).isEqualTo(303);
		assertThat(retry.get("Location")).isEqualTo("/logged-out");
	}

	@Test
	void shouldRejectTamperedRedirectLogoutResponseAndAllowSignedRetry() throws Exception
	{
		String tamperedResponse = logoutResponse.replace("ID=\"_response\"", "ID=\"_tampered\"");
		String encodedResponse = URLEncoder.encode(HttpRedirectBindingSupport.toURLParam(logoutResponse),
				StandardCharsets.UTF_8);
		String tamperedEncodedResponse = URLEncoder.encode(HttpRedirectBindingSupport.toURLParam(tamperedResponse),
				StandardCharsets.UTF_8);
		String redirectUrl = signedRedirectUrl().replace(encodedResponse, tamperedEncodedResponse);

		HttpTester.Response response = receiveResponse(redirectUrl);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.get("Content-Type")).startsWith("text/html");
		assertThat(response.getContent()).contains("An invalid logout response");
		assertThat(response.get("Location")).isNull();
		HttpTester.Response retry = receiveResponse(signedRedirectUrl());
		assertThat(retry.getStatus()).isEqualTo(303);
		assertThat(retry.get("Location")).isEqualTo("/logged-out");
	}

	private SAMLInternalLogoutContext pendingLogout()
	{
		SAMLInternalLogoutContext context = new SAMLInternalLogoutContext(new LoginSession(), null,
				(response, finishedContext) ->
				{
					response.setStatus(303);
					response.setHeader("Location", "/logged-out");
				}, new SessionParticipantTypesRegistry(), "logout-state");
		NameIDType subject = NameIDType.Factory.newInstance();
		subject.setStringValue("user");
		SAMLSessionParticipant participant = new SAMLSessionParticipant("https://peer.example", subject,
				"session", List.of(), "https://unity.example", "local-key", Set.of("peer-cert"));
		context.setCurrent(participant);
		context.setCurrentRequestId("_logoutRequest");
		return context;
	}

	private String signedRedirectUrl() throws Exception
	{
		return HttpRedirectBindingSupport.getSignedRedirectURL(SAMLMessageType.SAMLResponse,
				"logout-state", logoutResponse, CONSUMER_ADDRESS, signingKeys.getPrivate());
	}

	private HttpTester.Response receiveResponse(String redirectUrl) throws Exception
	{
		URI uri = URI.create(redirectUrl);
		return server.get(uri.getRawPath() + "?" + uri.getRawQuery());
	}
}
