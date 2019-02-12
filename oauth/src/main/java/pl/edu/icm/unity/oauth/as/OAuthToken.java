/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.client.ClientType;

import pl.edu.icm.unity.Constants;

/**
 * OAuth token as stored on the server to keep the state of the OAuth 'session'. It is used both to store access
 * and authZ tokens, as the data is nearly the same: in both cases we establish user's information a priori.
 * 
 * @author K. Benedyczak
 */
public class OAuthToken
{
	private String userInfo;
	private String openidInfo;
	private String authzCode;
	private String accessToken;
	private String refreshToken;
	private String[] effectiveScope;
	private String[] requestedScope;
	private long clientEntityId;
	private String redirectUri;
	private String subject;
	private String clientName;
	private String clientUsername;
	private int maxExtendedValidity;
	private int tokenValidity;
	private String responseType;
	private String audience;
	private String issuerUri;
	
	private String codeChallenge;
	private String codeChallengeMethod;
	private ClientType clientType;
	
	public OAuthToken()
	{
	}
	
	/**
	 * copy constructor
	 * @param source
	 */
	public OAuthToken(OAuthToken source)
	{
		setAccessToken(source.getAccessToken());
		setRefreshToken(source.getRefreshToken());
		setAuthzCode(source.getAuthzCode());
		setClientId(source.getClientId());
		setOpenidToken(source.getOpenidInfo());
		setRedirectUri(source.getRedirectUri());
		setEffectiveScope(source.getEffectiveScope());
		setUserInfo(source.getUserInfo());
		setClientName(source.getClientName());
		setClientUsername(source.getClientUsername());
		setSubject(source.getSubject());
		setMaxExtendedValidity(source.getMaxExtendedValidity());
		setTokenValidity(source.getTokenValidity());
		setResponseType(source.getResponseType());
		setRequestedScope(source.getRequestedScope());
		setAudience(source.getAudience());
		setIssuerUri(source.getIssuerUri());
		setCodeChallenge(source.getCodeChallenge());
		setCodeChallengeMethod(source.getCodeChallengeMethod());
		setClientType(source.getClientType());
	}
	
	public static OAuthToken getInstanceFromJson(byte[] json) 
	{
		try
		{
			return Constants.MAPPER.readValue(json, OAuthToken.class);
		} catch (IOException e)
		{
			throw new IllegalArgumentException("Can not parse token's JSON", e);
		}
	}
	
	@JsonIgnore
	public byte[] getSerialized() throws JsonProcessingException
	{
		return Constants.MAPPER.writeValueAsBytes(this);
	}


	public String getUserInfo()
	{
		return userInfo;
	}


	public void setUserInfo(String userInfo)
	{
		this.userInfo = userInfo;
	}

	public String getAuthzCode()
	{
		return authzCode;
	}

	public void setAuthzCode(String authzCode)
	{
		this.authzCode = authzCode;
	}

	public String getAccessToken()
	{
		return accessToken;
	}
	
	public String getRefreshToken()
	{
		return refreshToken;
	}

	public void setAccessToken(String accessToken)
	{
		this.accessToken = accessToken;
	}
	
	public void setRefreshToken(String refreshToken)
	{
		this.refreshToken = refreshToken;
	}

	public String[] getEffectiveScope()
	{
		return effectiveScope;
	}

	public void setEffectiveScope(String[] scope)
	{
		this.effectiveScope = scope;
	}

	public String getOpenidInfo()
	{
		return openidInfo;
	}

	public void setOpenidToken(String openidInfo)
	{
		this.openidInfo = openidInfo;
	}

	public long getClientId()
	{
		return clientEntityId;
	}

	public void setClientId(long clientId)
	{
		this.clientEntityId = clientId;
	}


	public String getRedirectUri()
	{
		return redirectUri;
	}

	public void setRedirectUri(String redirectUri)
	{
		this.redirectUri = redirectUri;
	}

	public String getSubject()
	{
		return subject;
	}
	
	public void setSubject(String subject)
	{
		this.subject = subject;
	}
	
	public String getResponseType()
	{
		return responseType;
	}

	public void setResponseType(String responseType)
	{
		this.responseType = responseType;
	}
	
	/**
	 * @return displayed name of the client or null if not defined
	 */
	public String getClientName()
	{
		return clientName;
	}

	/**
	 * Sets displayed name of the client
	 * @param clientName
	 */
	public void setClientName(String clientName)
	{
		this.clientName = clientName;
	}

	/**
	 * @return identity of the OAuth client, generally username identity.
	 */
	public String getClientUsername()
	{
		return clientUsername;
	}

	/**
	 * Sets the identity (username) of the OAuth client
	 * @param clientUsername
	 */
	public void setClientUsername(String clientUsername)
	{
		this.clientUsername = clientUsername;
	}

	public int getMaxExtendedValidity()
	{
		return maxExtendedValidity;
	}

	/**
	 * Sets to what time this tokens validity can be maximally extended.
	 * @param maxExtendedValidity
	 */
	public void setMaxExtendedValidity(int maxExtendedValidity)
	{
		this.maxExtendedValidity = maxExtendedValidity;
	}

	public int getTokenValidity()
	{
		return tokenValidity;
	}

	public void setTokenValidity(int tokenValidity)
	{
		this.tokenValidity = tokenValidity;
	}

	public String[] getRequestedScope()
	{
		return requestedScope;
	}

	public void setRequestedScope(String[] requestedScope)
	{
		this.requestedScope = requestedScope;
	}

	public String getAudience()
	{
		return audience;
	}

	public void setAudience(String audience)
	{
		this.audience = audience;
	}

	public String getIssuerUri()
	{
		return issuerUri;
	}

	public void setIssuerUri(String issuerUri)
	{
		this.issuerUri = issuerUri;
	}

	public String getCodeChallenge()
	{
		return codeChallenge;
	}

	public void setCodeChallenge(String codeChallenge)
	{
		this.codeChallenge = codeChallenge;
	}

	public String getCodeChallengeMethod()
	{
		return codeChallengeMethod;
	}

	public void setCodeChallengeMethod(String codeChallengeMethod)
	{
		this.codeChallengeMethod = codeChallengeMethod;
	}

	public ClientType getClientType()
	{
		return clientType == null ? ClientType.CONFIDENTIAL : clientType;
	}

	public void setClientType(ClientType clientType)
	{
		this.clientType = clientType;
	}
}
