/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class OAuthFederationEntityStatementServletTest
{
	private static final String ENTITY_ID = "https://rp.example.com";
	private static final String CALLBACK_URL = "https://rp.example.com/callback";
	private static final String AUTHENTICATOR_NAME = "myAuthenticator";
	private static final String RSA_KEYSTORE = "src/test/resources/pki/demoKeystore.p12";
	private static final String RSA_KEYSTORE_PASS = "the!unity";

	@Mock
	OAuthFederationMetadataManager manager;
	@Mock
	HttpServletRequest req;
	@Mock
	HttpServletResponse resp;

	OAuthFederationEntityStatementServlet servlet;

	@BeforeEach
	void setUp()
	{
		servlet = new OAuthFederationEntityStatementServlet(manager);
	}

	@Test
	void shouldReturn400WhenPathInfoIsNull() throws Exception
	{
		when(req.getPathInfo()).thenReturn(null);

		servlet.doGet(req, resp);

		verify(resp).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
	}

	@Test
	void shouldReturn400WhenPathDoesNotEndWithWellKnownSuffix() throws Exception
	{
		when(req.getPathInfo()).thenReturn("/" + AUTHENTICATOR_NAME + "/something-else");

		servlet.doGet(req, resp);

		verify(resp).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
	}

	@Test
	void shouldReturn400WhenAuthenticatorNameIsAbsentInPath() throws Exception
	{
		when(req.getPathInfo()).thenReturn(OAuthFederationEntityStatementServlet.WELL_KNOWN_SUFFIX);

		servlet.doGet(req, resp);

		verify(resp).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
	}

	@Test
	void shouldReturn404WhenAuthenticatorHasNoFederationConfig() throws Exception
	{
		when(req.getPathInfo()).thenReturn(
				"/" + AUTHENTICATOR_NAME + OAuthFederationEntityStatementServlet.WELL_KNOWN_SUFFIX);
		when(manager.getConfiguration(AUTHENTICATOR_NAME)).thenReturn(null);

		servlet.doGet(req, resp);

		verify(resp).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
	}

	@Test
	void shouldReturn500WhenEntityStatementGenerationFails() throws Exception
	{
		when(req.getPathInfo()).thenReturn(
				"/" + AUTHENTICATOR_NAME + OAuthFederationEntityStatementServlet.WELL_KNOWN_SUFFIX);
		OAuthFederationEntityStatementConfig config = new OAuthFederationEntityStatementConfig(
				ENTITY_ID, null, null, CALLBACK_URL, null, 3600);
		when(manager.getConfiguration(AUTHENTICATOR_NAME)).thenReturn(config);

		servlet.doGet(req, resp);

		verify(resp).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
	}

	@Test
	void shouldWriteEntityStatementJwtWithoutErrorOnSuccess() throws Exception
	{
		X509Credential credential = rsaCredential();
		OAuthFederationEntityStatementConfig config = new OAuthFederationEntityStatementConfig(
				ENTITY_ID, credential, null, CALLBACK_URL, null, 3600);
		when(req.getPathInfo()).thenReturn(
				"/" + AUTHENTICATOR_NAME + OAuthFederationEntityStatementServlet.WELL_KNOWN_SUFFIX);
		when(manager.getConfiguration(AUTHENTICATOR_NAME)).thenReturn(config);
		StringWriter sw = new StringWriter();
		lenient().when(resp.getWriter()).thenReturn(new PrintWriter(sw));

		servlet.doGet(req, resp);

		verify(resp, never()).sendError(anyInt(), anyString());
	}

	private static X509Credential rsaCredential() throws Exception
	{
		return new KeystoreCredential(RSA_KEYSTORE,
				RSA_KEYSTORE_PASS.toCharArray(), RSA_KEYSTORE_PASS.toCharArray(), null, "pkcs12");
	}
}
