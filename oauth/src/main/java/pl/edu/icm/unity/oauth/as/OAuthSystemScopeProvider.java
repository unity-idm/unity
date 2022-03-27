/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.nimbusds.openid.connect.sdk.OIDCScopeValue;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.oauth.api.Scope;
import pl.edu.icm.unity.oauth.api.SystemScopeProvider;

@Component
public class OAuthSystemScopeProvider implements SystemScopeProvider
{
	private static final String id = "OAuth";
	public static final String OPENID_SCOPE = OIDCScopeValue.OPENID.getValue();
	public static final String OFFLINE_ACCESS_SCOPE = OIDCScopeValue.OFFLINE_ACCESS.getValue();
	
	private final MessageSource msg;

	public OAuthSystemScopeProvider(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public List<Scope> getScopes()
	{
		return List.of(
				Scope.builder().withName(OPENID_SCOPE)
						.withDescription(msg.getMessage("OAuthSystemScopeProvider.openidScopeDesc")).build(),
				Scope.builder().withName(OFFLINE_ACCESS_SCOPE)
						.withDescription(msg.getMessage("OAuthSystemScopeProvider.offlineAccessScopeDesc")).build());
	}

	@Override
	public String getId()
	{
		return id;
	}

	public static Set<String> getScopeNames()
	{
		return Set.of(OPENID_SCOPE, OFFLINE_ACCESS_SCOPE);
	}
	
}
