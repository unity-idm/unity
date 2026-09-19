/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.DenyReason;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;

class HttpClientAssertionRetrievalTest
{
	@Test
	void shouldFailWhenNoCxfMessage()
	{
		var retrieval = retrievalWithMessage(null);

		AuthenticationResult result = retrieval.getAuthenticationResult(new Properties());

		assertThat(result.getStatus()).isEqualTo(Status.deny);
		assertThat(result.getDenyReason()).contains(DenyReason.undefinedCredential);
	}

	@Test
	void shouldFailWhenNoHttpRequestInMessage()
	{
		var retrieval = retrievalWithMessage(new MessageImpl());

		AuthenticationResult result = retrieval.getAuthenticationResult(new Properties());

		assertThat(result.getStatus()).isEqualTo(Status.deny);
		assertThat(result.getDenyReason()).contains(DenyReason.undefinedCredential);
	}

	@Test
	void shouldFailWhenNoFormParams()
	{
		MessageImpl message = new MessageImpl();
		message.put(AbstractHTTPDestination.HTTP_REQUEST, mock(HttpServletRequest.class));
		var retrieval = retrievalWithMessage(message);

		AuthenticationResult result = retrieval.getAuthenticationResult(new Properties());

		assertThat(result.getStatus()).isEqualTo(Status.deny);
		assertThat(result.getDenyReason()).contains(DenyReason.undefinedCredential);
	}

	@Test
	void shouldFailWhenAssertionFieldsMissing()
	{
		var retrieval = retrievalWithMessage(messageWithFormParams(formParams(null, null)));

		AuthenticationResult result = retrieval.getAuthenticationResult(new Properties());

		assertThat(result.getStatus()).isEqualTo(Status.deny);
		assertThat(result.getDenyReason()).contains(DenyReason.undefinedCredential);
	}

	@Test
	void shouldFailWhenAssertionTypeMismatch()
	{
		var retrieval = retrievalWithMessage(messageWithFormParams(
				formParams("urn:ietf:params:oauth:client-assertion-type:saml2-bearer", "some.jwt.token")));

		AuthenticationResult result = retrieval.getAuthenticationResult(new Properties());

		assertThat(result.getStatus()).isEqualTo(Status.deny);
		assertThat(result.getDenyReason()).contains(DenyReason.undefinedCredential);
	}

	@Test
	void shouldDelegateToExchangeWhenValidAssertionPresent()
	{
		ClientAssertionExchange exchange = mock(ClientAssertionExchange.class);
		AuthenticationResult expected = LocalAuthenticationResult.failed(new ResolvableError("test"));
		String assertionJwt = "header.payload.signature";
		when(exchange.verifyClientAssertion(eq(assertionJwt), any())).thenReturn(expected);

		HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getRequestURL()).thenReturn(new StringBuffer("https://example.com/token"));
		Message message = messageWithFormParams(req,
				formParams(HttpClientAssertionRetrieval.JWT_BEARER_TYPE, assertionJwt));

		var retrieval = retrievalWithMessageAndExchange(message, exchange);

		AuthenticationResult result = retrieval.getAuthenticationResult(new Properties());

		assertThat(result).isSameAs(expected);
	}

	@Test
	void shouldUrlDecodeAssertionFields()
	{
		ClientAssertionExchange exchange = mock(ClientAssertionExchange.class);
		String decoded = "header.payload.signature";
		String encoded = "header.payload.signature"; // already url-safe, no encoding needed
		when(exchange.verifyClientAssertion(eq(decoded), any()))
				.thenReturn(LocalAuthenticationResult.failed(new ResolvableError("ok")));

		HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getRequestURL()).thenReturn(new StringBuffer("https://example.com/token"));
		Message message = messageWithFormParams(req,
				formParams(HttpClientAssertionRetrieval.JWT_BEARER_TYPE, encoded));

		var retrieval = retrievalWithMessageAndExchange(message, exchange);

		AuthenticationResult result = retrieval.getAuthenticationResult(new Properties());

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	private static MultivaluedMap<String, String> formParams(String assertionType, String assertion)
	{
		MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
		if (assertionType != null)
			params.putSingle("client_assertion_type", assertionType);
		if (assertion != null)
			params.putSingle("client_assertion", assertion);
		return params;
	}

	private static Message messageWithFormParams(MultivaluedMap<String, String> params)
	{
		HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getRequestURL()).thenReturn(new StringBuffer("https://example.com/token"));
		return messageWithFormParams(req, params);
	}

	private static Message messageWithFormParams(HttpServletRequest req,
			MultivaluedMap<String, String> params)
	{
		MessageImpl message = new MessageImpl();
		message.put(AbstractHTTPDestination.HTTP_REQUEST, req);
		message.put(FormUtils.FORM_PARAM_MAP, params);
		return message;
	}

	private static HttpClientAssertionRetrieval retrievalWithMessage(Message message)
	{
		return new HttpClientAssertionRetrieval()
		{
			@Override
			protected Message getCurrentCxfMessage()
			{
				return message;
			}
		};
	}

	private static HttpClientAssertionRetrieval retrievalWithMessageAndExchange(Message message,
			ClientAssertionExchange exchange)
	{
		HttpClientAssertionRetrieval retrieval = new HttpClientAssertionRetrieval()
		{
			@Override
			protected Message getCurrentCxfMessage()
			{
				return message;
			}
		};
		retrieval.setCredentialExchange(exchange, "test");
		return retrieval;
	}
}
