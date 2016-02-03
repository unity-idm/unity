/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.io.IOException;

import pl.edu.icm.unity.Constants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

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
	private String[] scope;
	private long clientEntityId;
	private String redirectUri;
	private String subject;
	private String clientName;
	private String clientUsername;
	
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
		setAuthzCode(source.getAuthzCode());
		setClientId(source.getClientId());
		setOpenidToken(source.getOpenidInfo());
		setRedirectUri(source.getRedirectUri());
		setScope(source.getScope());
		setUserInfo(source.getUserInfo());
		setClientName(source.getClientName());
		setClientUsername(source.getClientUsername());
		setSubject(source.getSubject());
	}
	
	public static OAuthToken getInstanceFromJson(byte[] json) 
			throws JsonParseException, JsonMappingException, IOException
	{
		return Constants.MAPPER.readValue(json, OAuthToken.class);
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

	public void setAccessToken(String accessToken)
	{
		this.accessToken = accessToken;
	}

	public String[] getScope()
	{
		return scope;
	}


	public void setScope(String[] scope)
	{
		this.scope = scope;
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
}
