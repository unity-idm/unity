/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.profile;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import net.minidev.json.JSONObject;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.oauth.client.AttributeFetchResult;
import pl.edu.icm.unity.oauth.client.HttpRequestConfigurer;
import pl.edu.icm.unity.oauth.client.UserProfileFetcher;

public class OpenIdProfileFetcher implements UserProfileFetcher
{
	@Override
	public AttributeFetchResult fetchProfile(BearerAccessToken accessToken, String userInfoEndpoint,
			ProfileFetcherConfig fetcherConfig, Map<String, List<String>> attributesSoFar) throws Exception
	{
		UserInfoRequest uiRequest = new UserInfoRequest(new URI(userInfoEndpoint), accessToken);
		HTTPRequest httpsRequest = new HttpRequestConfigurer().secureRequest(uiRequest.toHTTPRequest(),
				fetcherConfig.validator(), fetcherConfig.hostNameCheckingMode());
		HTTPResponse uiHttpResponse = httpsRequest.send();
		UserInfoResponse uiResponse = UserInfoResponse.parse(uiHttpResponse);
		if (uiResponse instanceof UserInfoErrorResponse)
			throw new AuthenticationException("Authentication was successful, but an error "
					+ "occurred during user information endpoint query: "
					+ uiHttpResponse.getBody());

		UserInfoSuccessResponse uiResponseS = (UserInfoSuccessResponse) uiResponse;
		if (uiResponseS.getUserInfoJWT() != null)
		{
			JWTClaimsSet claimSet = uiResponseS.getUserInfoJWT().getJWTClaimsSet();
			JSONObject raw = new JSONObject(claimSet.getClaims());
			return ProfileFetcherUtils.fetchFromJsonObject(raw);
		} else
		{
			UserInfo ui = uiResponseS.getUserInfo();
			JWTClaimsSet claimSet = ui.toJWTClaimsSet();
			JSONObject raw = new JSONObject(claimSet.getClaims());
			return ProfileFetcherUtils.fetchFromJsonObject(raw);
		}
	}
}
