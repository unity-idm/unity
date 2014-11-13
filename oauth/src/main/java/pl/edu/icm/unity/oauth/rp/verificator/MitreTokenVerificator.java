/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.verificator;

import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map.Entry;

import net.minidev.json.JSONObject;

import org.apache.log4j.Logger;

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
 * Validates the token against MITRE compatible token verification endpoint. 
 * The protocol is described in the 
 * https://tools.ietf.org/html/draft-ietf-oauth-introspection-00
 * 
 * @author K. Benedyczak
 */
public class MitreTokenVerificator implements TokenVerificatorProtocol
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, MitreTokenVerificator.class);
	private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZ";
	private OAuthRPProperties config;
	
	public MitreTokenVerificator(OAuthRPProperties config)
	{
		this.config = config;
	}

	@Override
	public TokenStatus checkToken(BearerAccessToken token) throws Exception
	{
		String verificationEndpoint = config.getValue(OAuthRPProperties.VERIFICATION_ENDPOINT);
		
		HTTPRequest httpReqRaw = new HTTPRequest(Method.GET, new URL(verificationEndpoint));
		
		StringBuilder queryB = new StringBuilder();
		queryB.append("client_id=").append(URLEncoder.encode(config.getValue(OAuthRPProperties.CLIENT_ID), 
				"UTF-8"));
		queryB.append("&client_secret=").append(URLEncoder.encode(
				config.getValue(OAuthRPProperties.CLIENT_SECRET), "UTF-8"));
		queryB.append("&token=").append(URLEncoder.encode(token.getValue(), "UTF-8"));
		httpReqRaw.setQuery(queryB.toString());
		
		ServerHostnameCheckingMode checkingMode = config.getEnumValue(
				OAuthRPProperties.CLIENT_HOSTNAME_CHECKING, ServerHostnameCheckingMode.class);
		HTTPRequest httpReq = new CustomHTTPSRequest(httpReqRaw, config.getValidator(), checkingMode);

		HTTPResponse resp = httpReq.send();
		
		if (resp.getStatusCode() != 200)
		{
			throw new AuthenticationException("Token status query was not successful: " + 
					resp.getStatusCode());
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
		boolean valid = false;
		String subject = null;
		for (Entry<String, Object> entry: status.entrySet())
		{
			if (entry.getValue() == null)
				continue;
			
			if ("exp".equals(entry.getKey()))
			{
				DateFormat fd = new SimpleDateFormat(DATE_PATTERN);
				exp = fd.parse(entry.getValue().toString());
			} else if ("scope".equals(entry.getKey()))
			{
				String scopes = (String) entry.getValue();
				for (String s: scopes.split(" "))
					scope.add(s);
			} else if ("active".equals(entry.getKey()))
			{
				valid = Boolean.parseBoolean(entry.getValue().toString());
			} else if ("sub".equals(entry.getKey()))
			{
				subject = entry.getValue().toString();
			}
		}
		
		if (exp != null && new Date().after(exp))
		{
			log.trace("The token information states that the token expired at " + exp);
			return new TokenStatus();
		}
		
		return new TokenStatus(valid, exp, scope, subject);
	}

}
