/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.util.List;
import java.util.Map;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import pl.edu.icm.unity.oauth.BaseRemoteASProperties;

public interface UserProfileFetcher
{
	/**
	 * Retrieves attributes from user's profile which is accessed in implementation specific way.
	 * @param accessToken
	 * @param providerConfig
	 * @return
	 * @throws Exception
	 */
	AttributeFetchResult fetchProfile(BearerAccessToken accessToken, String userInfoEndpoint,
			BaseRemoteASProperties providerConfig, Map<String, List<String>> attributesFromACResponse) 
					throws Exception;
}
