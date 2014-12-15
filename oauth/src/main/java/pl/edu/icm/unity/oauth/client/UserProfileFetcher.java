/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import net.minidev.json.JSONObject;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.utils.Log;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

/**
 * Fetches user profile from OpenID compliant profile endpoint
 * @author K. Benedyczak
 */
public class UserProfileFetcher
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, UserProfileFetcher.class);
	
	public enum ClientAuthnMode {secretPost, secretBasic};
	
	/**
	 * Downloads from profile endpoint and insert parsed attributes into the ret parameter.
	 * @param accessToken
	 * @param userInfoEndpoint
	 * @param ret
	 * @param checkingMode
	 * @param validator
	 * @throws AuthenticationException
	 * @throws SerializeException
	 * @throws IOException
	 * @throws ParseException
	 * @throws java.text.ParseException
	 */
	public static void fetchOpenIdUserInfo(BearerAccessToken accessToken, URI userInfoEndpoint, 
			Map<String, String> ret, 
			ServerHostnameCheckingMode checkingMode, X509CertChainValidator validator) 
			throws AuthenticationException, SerializeException, IOException, 
			ParseException, java.text.ParseException
	{
		UserInfoRequest uiRequest = new UserInfoRequest(userInfoEndpoint, accessToken);
		HTTPRequest httpsRequest = new CustomHTTPSRequest(uiRequest.toHTTPRequest(), validator, checkingMode); 
		HTTPResponse uiHttpResponse = httpsRequest.send();
		UserInfoResponse uiResponse = UserInfoResponse.parse(uiHttpResponse);
		if (uiResponse instanceof UserInfoErrorResponse)
		{
			String code = uiHttpResponse.getContent();
			throw new AuthenticationException("Authentication was successful, but an error "
					+ "occurred during user information endpoint query: " + 
					code);
		}
		UserInfoSuccessResponse uiResponseS = (UserInfoSuccessResponse) uiResponse;
		if (uiResponseS.getUserInfoJWT() != null)
		{
			ReadOnlyJWTClaimsSet claimSet = uiResponseS.getUserInfoJWT().getJWTClaimsSet();
			toAttributes(claimSet, ret);
		} else
		{
			UserInfo ui = uiResponseS.getUserInfo();
			JWTClaimsSet claimSet = ui.toJWTClaimsSet();
			toAttributes(claimSet, ret);
		}
	}

	/**
	 * Downloads user information from a plain profile endpoint. 
	 * Parsed attributes are inserted into the ret parameter.
	 * @param accessToken
	 * @param selectedMethod
	 * @param userInfoEndpoint
	 * @param ret
	 * @param context
	 * @throws AuthenticationException
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void fetchUserInfo(BearerAccessToken accessToken, ClientAuthnMode selectedMethod,
			String userInfoEndpoint, Map<String, String> ret,
			ServerHostnameCheckingMode checkingMode, X509CertChainValidator validator) 
					throws AuthenticationException, IOException, ParseException
	{
		HTTPRequest httpReqRaw = new HTTPRequest(Method.GET, new URL(userInfoEndpoint));
		
		HTTPRequest httpReq = new CustomHTTPSRequest(httpReqRaw, validator, checkingMode);
		if (selectedMethod == ClientAuthnMode.secretPost)
			httpReq.setQuery("access_token=" + accessToken.getValue());
		else
			httpReq.setAuthorization(accessToken.toAuthorizationHeader());
		
		HTTPResponse resp = httpReq.send();
		
		if (resp.getStatusCode() != 200)
		{
			throw new AuthenticationException("Authentication was successful "
					+ "but there was a problem fetching user's profile information: " + 
					resp.getContent());
		}
		if (log.isTraceEnabled())
			log.trace("Received user's profile:\n" + resp.getContent());

		if (resp.getContentType() == null || 
				!"application/json".equals(resp.getContentType().getBaseType().toString()))
			throw new AuthenticationException("Authentication was successful "
					+ "but there was a problem fetching user's profile information. "
					+ "It has non-JSON content type: " + resp.getContentType());
		
		JSONObject profile = resp.getContentAsJSONObject();
		
		for (Entry<String, Object> entry: profile.entrySet())
		{
			if (entry.getValue() != null)
				ret.put(entry.getKey(), entry.getValue().toString());
		}
	}
	

	
	public static void toAttributes(ReadOnlyJWTClaimsSet claimSet, Map<String, String> attributes)
	{
		Map<String, Object> claims = claimSet.getAllClaims();
		for (Map.Entry<String, Object> claim: claims.entrySet())
		{
			if (claim.getValue() != null)
				attributes.put(claim.getKey(), claim.getValue().toString());
		}
	}
}
