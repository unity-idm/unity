/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.local;

import java.util.Arrays;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.oauth.rp.retrieval.BearerRetrievalBase;
import pl.edu.icm.unity.rest.authn.JAXRSAuthentication;

@PrototypeComponent
public class RESTLocalBearerTokenRetrieval extends BearerRetrievalBase implements JAXRSAuthentication
{
	public static final String NAME = "rest-oauth-bearer-password";
	public static final String DESC = "RESTLocalBearerTokenRetrieval.desc";

	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, RESTLocalBearerTokenRetrieval.class);

	public RESTLocalBearerTokenRetrieval()
	{
		super(JAXRSAuthentication.NAME);
	}

	@Override
	public String getSerializedConfiguration()
	{
		return "";
	}

	@Override
	public void setSerializedConfiguration(String config)
	{
	}

	@Override
	public AbstractPhaseInterceptor<Message> getInterceptor()
	{
		return null;
	}

	@Override
	protected BearerAccessToken getTokenCredential(Logger ignored)
	{
		Message message = getCurrentMessage();
		if (message == null)
			return null;
		HttpServletRequest request = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
		if (request == null)
			return null;

		String authorization = request.getHeader("Authorization");
		if (authorization == null)
			return null;

		Optional<String> bearerSegment = extractBearerSegment(authorization);
		if (bearerSegment.isEmpty())
			return null;

		try
		{
			return BearerAccessToken.parse(bearerSegment.get());
		} catch (ParseException e)
		{
			log.debug("Received HTTP authorization header, but it is not a valid Bearer access token", e);
			return null;
		}
	}

	private Optional<String> extractBearerSegment(String headerValue)
	{
		return Arrays.stream(headerValue.split(","))
			.map(String::trim)
			.map(this::normaliseBearerSegment)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst();
	}

	private Optional<String> normaliseBearerSegment(String authSegment)
	{
		if (!authSegment.regionMatches(true, 0, "Bearer", 0, "Bearer".length()))
		{
			return Optional.empty();
		}
		int spaceIdx = authSegment.indexOf(' ');
		if (spaceIdx < 0 || spaceIdx == authSegment.length() - 1)
		{
			return Optional.empty();
		}
		String token = authSegment.substring(spaceIdx + 1).trim();
		if (token.isEmpty())
		{
			return Optional.empty();
		}
		return Optional.of("Bearer " + token);
	}

	Message getCurrentMessage()
	{
		return PhaseInterceptorChain.getCurrentMessage();
	}

	@Component
	public static class Factory extends AbstractCredentialRetrievalFactory<RESTLocalBearerTokenRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<RESTLocalBearerTokenRetrieval> factory)
		{
			super(NAME, DESC, JAXRSAuthentication.NAME, factory, LocalAccessTokenExchange.ID);
		}
	}
}
