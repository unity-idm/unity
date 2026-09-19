/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.nimbusds.openid.connect.sdk.OIDCScopeValue;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.oauth.api.Scope;
import pl.edu.icm.unity.oauth.api.SystemScopeProvider;
import pl.edu.icm.unity.oauth.as.token.access.AccessTokenResource;

@Component
public class OAuthSystemScopeProvider implements SystemScopeProvider
{
	public static final String id = "OAuth";
	public static final String OPENID_SCOPE = OIDCScopeValue.OPENID.getValue();
	public static final String OFFLINE_ACCESS_SCOPE = OIDCScopeValue.OFFLINE_ACCESS.getValue();
	public static final String TOKEN_EXCHANGE_SCOPE = AccessTokenResource.EXCHANGE_SCOPE;

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
						.withDescription(msg.getMessage("OAuthSystemScopeProvider.offlineAccessScopeDesc")).build(),
				Scope.builder().withName(TOKEN_EXCHANGE_SCOPE)
						.withDescription(msg.getMessage("OAuthSystemScopeProvider.tokenExchangeScopeDesc")).build());
	}

	@Override
	public String getId()
	{
		return id;
	}

	public static Set<String> getScopeNames()
	{
		return Set.of(OPENID_SCOPE, OFFLINE_ACCESS_SCOPE, TOKEN_EXCHANGE_SCOPE);
	}
	
}
