/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console.tokens;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.webui.common.grid.FilterableEntry;

class OAuthTokenBean implements FilterableEntry
{
	private Token token;
	private OAuthToken oauthToken;
	private UnityMessageSource msg;
	private String owner;

	public OAuthTokenBean(Token token, UnityMessageSource msg, String owner)
	{
		this.token = token;
		this.msg = msg;
		this.oauthToken = OAuthToken.getInstanceFromJson(token.getContents());
		this.owner = owner;
	}

	public String getType()
	{
		try
		{
			return msg.getMessage("OAuthTokenType." + token.getType());
		} catch (Exception e)
		{
			return token.getType();
		}
	}

	String getRealType()
	{
		return token.getType();
	}

	public String getCreateTime()
	{
		return TimeUtil.formatStandardInstant(token.getCreated().toInstant());
	}

	public String getExpires()
	{
		return TimeUtil.formatStandardInstant(token.getExpires().toInstant());
	}

	public String getValue()
	{
		return token.getValue();
	}

	public String getServerId()
	{
		return oauthToken.getIssuerUri();
	}

	public String getRefreshToken()
	{
		// show refresh token only for access token
		boolean isRefreshToken = token.getType().equals(OAuthProcessor.INTERNAL_REFRESH_TOKEN);
		return oauthToken.getRefreshToken() != null && !isRefreshToken ? oauthToken.getRefreshToken() : "";
	}

	public boolean getHasIdToken()
	{
		return oauthToken.getOpenidInfo() != null;
	}

	public String getClientName()
	{
		return oauthToken.getClientName() != null && !oauthToken.getClientName().isEmpty()
				? oauthToken.getClientName()
				: oauthToken.getClientUsername();
	}

	public String getScopes()
	{
		return Stream.of(oauthToken.getEffectiveScope()).collect(Collectors.joining(", "));
	}

	public String getOwner()
	{
		return owner;
	}

	Token getToken()
	{
		return token;
	}

	OAuthToken getOAuthToken()
	{
		return oauthToken;
	}

	@Override
	public boolean anyFieldContains(String searched, UnityMessageSource msg)
	{
		String textLower = searched.toLowerCase();

		if (getType() != null && getType().toLowerCase().contains(textLower))
			return true;

		if (getValue() != null && getValue().toLowerCase().contains(textLower))
			return true;

		if (getOwner() != null && getOwner().toLowerCase().contains(textLower))
			return true;

		if (getClientName() != null && getClientName().toLowerCase().contains(textLower))
			return true;

		if (getExpires() != null && getExpires().toLowerCase().contains(textLower))
			return true;

		if (getCreateTime() != null && getCreateTime().toLowerCase().contains(textLower))
			return true;

		if (getScopes() != null && getScopes().toLowerCase().contains(textLower))
			return true;

		if (getServerId() != null && getServerId().toLowerCase().contains(textLower))
			return true;

		if (getRefreshToken() != null && getRefreshToken().toLowerCase().contains(textLower))
			return true;

		if (String.valueOf(getHasIdToken()).toLowerCase().contains(textLower))
			return true;

		return false;
	}
}