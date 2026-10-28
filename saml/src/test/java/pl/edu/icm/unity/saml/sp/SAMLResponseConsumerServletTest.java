package pl.edu.icm.unity.saml.sp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.eclipse.jetty.http.HttpTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.unicore.samly2.binding.HttpRedirectBindingSupport;
import io.imunity.vaadin.endpoint.common.RemoteRedirectedAuthnResponseProcessingFilter;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext.FactorOrder;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RedirectedAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.SharedRemoteAuthenticationContextStore;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.saml.ServletTestServer;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

class SAMLResponseConsumerServletTest
{
	private final SharedRemoteAuthenticationContextStore contextStore = new SharedRemoteAuthenticationContextStore();
	private RemoteAuthnContext context;
	private ServletTestServer server;

	@BeforeEach
	void setUp() throws Exception
	{
		UnityServerConfiguration configuration = mock(UnityServerConfiguration.class);
		when(configuration.getIntValue(UnityServerConfiguration.MAX_REMOTE_AUTHN_TIME_S)).thenReturn(300);
		SamlContextManagement contextManagement = new SamlContextManagement(configuration);
		AuthenticationStepContext authenticationStep = new AuthenticationStepContext(new AuthenticationRealm(),
				null, AuthenticationOptionKey.authenticatorOnlyKey("saml"), FactorOrder.FIRST, "/login", null);
		RedirectedAuthnState state = new RedirectedAuthnState(authenticationStep, null,
				new LoginMachineDetails("ip", "os", "browser"), "https://unity.example/login",
				AuthenticationTriggeringContext.authenticationTriggeredFirstFactor());
		context = new RemoteAuthnContext(FakeTrustedIdPConfiguration.get(), FakeSAMLSPConfiguration.get(),
				state, null, "_request", "https://unity.example/login");
		contextManagement.addAuthnContext(context);
		server = new ServletTestServer(new SAMLResponseConsumerServlet(contextManagement, contextStore),
				SAMLResponseConsumerServlet.PATH);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		server.close();
	}

	@Test
	void shouldRejectUnsignedRedirectResponseAndAllowRetryUsingPost() throws Exception
	{
		ResponseDocument document = ResponseDocument.Factory.parse(signedResponse());
		document.getResponse().unsetSignature();
		for (var assertion : document.getResponse().getAssertionArray())
			assertion.unsetSignature();
		String target = SAMLResponseConsumerServlet.PATH + "?SAMLResponse="
				+ URLEncoder.encode(HttpRedirectBindingSupport.toURLParam(document.xmlText()), StandardCharsets.UTF_8)
				+ "&RelayState=" + context.getRelayState();

		HttpTester.Response response = server.get(target);

		assertThat(response.getStatus()).isEqualTo(405);
		assertThat(response.get("Allow")).isEqualTo("POST");
		assertThat(response.get("Location")).isNull();
		assertThat(context.getResponse()).isNull();
		HttpTester.Response retry = server.post(SAMLResponseConsumerServlet.PATH, postForm(signedResponse()));
		assertThat(retry.getStatus()).isEqualTo(302);
		assertThat(retry.get("Location")).isEqualTo(loginReturnUrl());
	}

	@Test
	void shouldRejectMalformedRedirectResponseAndAllowRetryUsingPost() throws Exception
	{
		String target = SAMLResponseConsumerServlet.PATH
				+ "?SAMLResponse=invalid-encoding&RelayState=" + context.getRelayState();

		HttpTester.Response response = server.get(target);

		assertThat(response.getStatus()).isEqualTo(405);
		assertThat(response.get("Location")).isNull();
		HttpTester.Response retry = server.post(SAMLResponseConsumerServlet.PATH, postForm(signedResponse()));
		assertThat(retry.getStatus()).isEqualTo(302);
		assertThat(retry.get("Location")).isEqualTo(loginReturnUrl());
	}

	@Test
	void shouldMakePostResponseAvailableToLoginEndpointAndRejectReplay() throws Exception
	{
		String samlResponse = signedResponse();
		String form = postForm(samlResponse);

		HttpTester.Response response = server.post(SAMLResponseConsumerServlet.PATH, form);

		assertThat(response.getStatus()).isEqualTo(302);
		assertThat(response.get("Location")).isEqualTo(loginReturnUrl());
		RemoteAuthnContext received = (RemoteAuthnContext)
				contextStore.getAndRemoveAuthnContext(context.getRelayState());
		assertThat(received.getResponse()).isEqualTo(samlResponse);
		HttpTester.Response replay = server.post(SAMLResponseConsumerServlet.PATH, form);
		assertThat(replay.getStatus()).isEqualTo(400);
		assertThat(replay.get("Location")).isNull();
	}

	private String signedResponse() throws Exception
	{
		return Files.readString(Path.of("src/test/resources/responseDocSigned.xml"));
	}

	private String postForm(String samlResponse)
	{
		String encodedResponse = Base64.getEncoder().encodeToString(samlResponse.getBytes(StandardCharsets.UTF_8));
		return "SAMLResponse=" + URLEncoder.encode(encodedResponse, StandardCharsets.UTF_8)
				+ "&RelayState=" + context.getRelayState();
	}

	private String loginReturnUrl()
	{
		return "https://unity.example/login?"
				+ RemoteRedirectedAuthnResponseProcessingFilter.CONTEXT_ID_HTTP_PARAMETER
				+ "=" + context.getRelayState();
	}
}
