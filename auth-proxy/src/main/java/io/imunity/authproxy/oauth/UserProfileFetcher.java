/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.authproxy.oauth;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.authproxy.UserAttributes;
import net.minidev.json.JSONObject;
import pl.edu.icm.unity.base.utils.Log;

class UserProfileFetcher
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, UserProfileFetcher.class);

	UserAttributes fetchProfile(BearerAccessToken accessToken,
			URI userInfoEndpoint, X509CertChainValidator validator,
			Map<String, List<String>> attributesSoFar) throws IOException, ParseException
	{
		
		HTTPRequest httpReqRaw = new HTTPRequest(Method.GET, userInfoEndpoint.toURL());
		HTTPRequest httpReq = new CustomHTTPSRequest(httpReqRaw,
				validator, ServerHostnameCheckingMode.FAIL);
		httpReq.setAuthorization(accessToken.toAuthorizationHeader());
		httpReq.setAccept("application/json");
		
		HTTPResponse resp = httpReq.send();

		if (resp.getStatusCode() != 200)
		{
			throw new AuthenticationException("Authentication was successful "
					+ "but there was a problem fetching user's profile information: "
					+ resp.getContent());
		}
		if (log.isTraceEnabled())
			log.trace("Received user's profile:\n" + resp.getContent());

		if (resp.getContentType() == null || !"application/json"
				.equals(resp.getContentType().getBaseType().toString()))
			throw new AuthenticationException("Authentication was successful "
					+ "but there was a problem fetching user's profile information. "
					+ "It has non-JSON content type: " + resp.getContentType());

		JSONObject profile = resp.getContentAsJSONObject();
	
		return ProfileFetcherUtils.fetchFromJsonObject(profile);
	}
}
