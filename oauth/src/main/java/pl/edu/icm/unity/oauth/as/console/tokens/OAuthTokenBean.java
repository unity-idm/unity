/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console.tokens;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.nimbusds.jwt.SignedJWT;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.BearerJWTAccessToken;
import pl.edu.icm.unity.oauth.as.token.OAuthRefreshTokenRepository;
import pl.edu.icm.unity.webui.common.grid.FilterableEntry;

class OAuthTokenBean implements FilterableEntry
{
	private Token token;
	private OAuthToken oauthToken;
	private MessageSource msg;
	private String owner;

	public OAuthTokenBean(Token token, MessageSource msg, String owner)
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
		return token.getExpires() == null ? "-" : TimeUtil.formatStandardInstant(token.getExpires().toInstant());
	}

	public String getId()
	{
		return token.getValue();
	}

	public String getTokenValue()
	{
		return isRefreshToken() ? oauthToken.getRefreshToken() : oauthToken.getAccessToken();
	}

	public String getServerId()
	{
		return oauthToken.getIssuerUri();
	}

	public String getAssociatedRefreshTokenForAccessToken()
	{
		// show refresh token only for access token
		return oauthToken.getRefreshToken() != null && !isRefreshToken() ? oauthToken.getRefreshToken() : "";
	}

	public Optional<SignedJWT> getJWT()
	{
		return BearerJWTAccessToken.tryParseJWT(oauthToken.getAccessToken());
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
	public boolean anyFieldContains(String searched, MessageSource msg)
	{
		String textLower = searched.toLowerCase();

		if (getType() != null && getType().toLowerCase().contains(textLower))
			return true;

		if (getId() != null && getId().toLowerCase().contains(textLower))
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

		if (getAssociatedRefreshTokenForAccessToken() != null && getAssociatedRefreshTokenForAccessToken().toLowerCase().contains(textLower))
			return true;

		if (String.valueOf(getHasIdToken()).toLowerCase().contains(textLower))
			return true;

		return false;
	}

	public boolean isRefreshToken()
	{
		return OAuthRefreshTokenRepository.isRefreshToken(token);
	}
}