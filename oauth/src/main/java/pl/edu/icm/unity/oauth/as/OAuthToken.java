/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
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
	private String firstRefreshRollingToken;
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
	@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
	private List<String> audience;
	private String issuerUri;
	private ClientType clientType;
	private PKCSInfo pkcsInfo;

	
	public OAuthToken()
	{
		pkcsInfo = new PKCSInfo();
	}
	
	/**
	 * copy constructor
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
		pkcsInfo = new PKCSInfo(source.pkcsInfo);
		setClientType(source.getClientType());
		setFirstRefreshRollingToken(source.getFirstRefreshRollingToken());
		
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

	public List<String> getAudience()
	{
		return audience;
	}

	public void setAudience(List<String> audience)
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
	
	public ClientType getClientType()
	{
		return clientType == null ? ClientType.CONFIDENTIAL : clientType;
	}

	public void setClientType(ClientType clientType)
	{
		this.clientType = clientType;
	}
	
	public PKCSInfo getPkcsInfo()
	{
		return pkcsInfo;
	}

	public void setPkcsInfo(PKCSInfo pkcsInfo)
	{
		this.pkcsInfo = pkcsInfo;
	}

	public String getFirstRefreshRollingToken()
	{
		return firstRefreshRollingToken;
	}

	public void setFirstRefreshRollingToken(String firstRefreshRollingToken)
	{
		this.firstRefreshRollingToken = firstRefreshRollingToken;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(effectiveScope);
		result = prime * result + Arrays.hashCode(requestedScope);
		result = prime * result + Objects.hash(accessToken, audience, authzCode, clientEntityId, clientName,
				clientType, clientUsername, issuerUri, maxExtendedValidity, openidInfo, pkcsInfo,
				redirectUri, refreshToken, responseType, subject, tokenValidity, userInfo, firstRefreshRollingToken);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OAuthToken other = (OAuthToken) obj;
		return Objects.equals(accessToken, other.accessToken) && Objects.equals(audience, other.audience)
				&& Objects.equals(authzCode, other.authzCode) && clientEntityId == other.clientEntityId
				&& Objects.equals(clientName, other.clientName) && clientType == other.clientType
				&& Objects.equals(clientUsername, other.clientUsername)
				&& Arrays.equals(effectiveScope, other.effectiveScope)
				&& Objects.equals(issuerUri, other.issuerUri)
				&& maxExtendedValidity == other.maxExtendedValidity
				&& Objects.equals(openidInfo, other.openidInfo)
				&& Objects.equals(pkcsInfo, other.pkcsInfo)
				&& Objects.equals(redirectUri, other.redirectUri)
				&& Objects.equals(refreshToken, other.refreshToken)
				&& Arrays.equals(requestedScope, other.requestedScope)
				&& Objects.equals(responseType, other.responseType)
				&& Objects.equals(subject, other.subject) && tokenValidity == other.tokenValidity
				&& Objects.equals(userInfo, other.userInfo)
				&& Objects.equals(firstRefreshRollingToken, other.firstRefreshRollingToken);
	}


	@Override
	public String toString()
	{
		return "OAuthToken [userInfo=" + userInfo + ", openidInfo=" + openidInfo + ", authzCode=" + authzCode
				+ ", accessToken=" + accessToken + ", refreshToken=" + refreshToken + ", firstRefreshRollingToken=" + firstRefreshRollingToken
				+ ", effectiveScope=" + Arrays.toString(effectiveScope) + ", requestedScope="
				+ Arrays.toString(requestedScope) + ", clientEntityId=" + clientEntityId
				+ ", redirectUri=" + redirectUri + ", subject=" + subject + ", clientName=" + clientName
				+ ", clientUsername=" + clientUsername + ", maxExtendedValidity=" + maxExtendedValidity
				+ ", tokenValidity=" + tokenValidity + ", responseType=" + responseType + ", audience="
				+ audience + ", issuerUri=" + issuerUri + ", clientType=" + clientType + ", pkcsInfo="
				+ pkcsInfo + "]";
	}

	public static class PKCSInfo
	{
		private String codeChallenge;
		private String codeChallengeMethod;
		
		PKCSInfo(PKCSInfo source)
		{
			this.codeChallenge = source.codeChallenge;
			this.codeChallengeMethod = source.codeChallengeMethod;
		}
		
		PKCSInfo()
		{
		}
		
		public PKCSInfo(String codeChallenge, String codeChallengeMethod)
		{
			this.codeChallenge = codeChallenge;
			this.codeChallengeMethod = codeChallengeMethod;
		}

		public String getCodeChallenge()
		{
			return codeChallenge;
		}

		public String getCodeChallengeMethod()
		{
			return codeChallengeMethod;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(codeChallenge, codeChallengeMethod);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PKCSInfo other = (PKCSInfo) obj;
			return Objects.equals(codeChallenge, other.codeChallenge)
					&& Objects.equals(codeChallengeMethod, other.codeChallengeMethod);
		}

		@Override
		public String toString()
		{
			return "PKCSInfo [codeChallenge=" + codeChallenge + ", codeChallengeMethod="
					+ codeChallengeMethod + "]";
		}
	}
}
