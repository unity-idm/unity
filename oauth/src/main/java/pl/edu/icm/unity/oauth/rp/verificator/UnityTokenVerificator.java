/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.verificator;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import net.minidev.json.JSONObject;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.oauth.as.token.TokenInfoResource;
import pl.edu.icm.unity.oauth.client.CustomHTTPSRequest;
import pl.edu.icm.unity.oauth.rp.OAuthRPProperties;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.utils.Log;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

/**
 * Verifies the token against Unity's own /tokeninfo path of the OAuth token endpoint.
 * The verification is simple: the JSON document is fetched. The error is treated as invalid token.
 * The response contains valid token's details. 
 * 
 * @author K. Benedyczak
 */
public class UnityTokenVerificator implements TokenVerificatorProtocol
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, UnityTokenVerificator.class);
	
	private OAuthRPProperties config;
	
	public UnityTokenVerificator(OAuthRPProperties config)
	{
		this.config = config;
	}

	@Override
	public TokenStatus checkToken(BearerAccessToken token) throws Exception
	{
		String verificationEndpoint = config.getValue(OAuthRPProperties.VERIFICATION_ENDPOINT);
		
		HTTPRequest httpReqRaw = new HTTPRequest(Method.GET, new URL(verificationEndpoint));
		
		ServerHostnameCheckingMode checkingMode = config.getEnumValue(
				OAuthRPProperties.CLIENT_HOSTNAME_CHECKING, ServerHostnameCheckingMode.class);
		HTTPRequest httpReq = new CustomHTTPSRequest(httpReqRaw, config.getValidator(), checkingMode);
		httpReq.setAuthorization(token.toAuthorizationHeader());
		
		HTTPResponse resp = httpReq.send();
		
		if (resp.getStatusCode() != 200)
		{
			if (log.isTraceEnabled())
				log.trace("Access token is invalid, HTTP status is: " + resp.getStatusCode());
			return new TokenStatus();
		}
		
		if (log.isTraceEnabled())
			log.trace("Received tokens's status:\n" + resp.getContent());

		if (resp.getContentType() == null || 
				!"application/json".equals(resp.getContentType().getBaseType().toString()))
			throw new AuthenticationException("Token status query was successful "
					+ "but it has non-JSON content type: " + resp.getContentType());
		
		JSONObject status = resp.getContentAsJSONObject();
		
		Date exp = null;
		Scope scope = new Scope();
		String subject = null;
		for (Entry<String, Object> entry: status.entrySet())
		{
			if (entry.getValue() == null)
				continue;
			
			if (TokenInfoResource.EXPIRATION.equals(entry.getKey()))
			{
				long expSec = Long.parseLong(entry.getValue().toString());
				exp = new Date(expSec*1000);
			} else if (TokenInfoResource.SCOPE.equals(entry.getKey()))
			{
				@SuppressWarnings("unchecked")
				List<String> scopes = (List<String>) entry.getValue();
				for (String s: scopes)
					scope.add(s);
			} else if (TokenInfoResource.SUBJECT.equals(entry.getKey()))
			{
				subject = entry.getValue().toString();
			}
		}
		
		if (exp != null && new Date().after(exp))
		{
			log.trace("The token information states that the token expired at " + exp);
			return new TokenStatus();
		}
		
		return new TokenStatus(true, exp, scope, subject);
	}

}
