/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.base.Constants;

/**
 * RFC 8628 device code bookkeeping (polling state), as stored on the server while a device
 * authorization is pending, approved or denied. Embeds the {@link OAuthToken} carrying the actual
 * token data that will be issued once the flow completes.
 */
public class DeviceCodeToken
{
	private DeviceCodeStatus deviceCodeStatus;
	private String userCode;
	private Instant lastPolledAt;
	private int currentPollInterval;
	private Long subjectEntityId;
	private OAuthToken oauthToken;

	public DeviceCodeToken()
	{
		oauthToken = new OAuthToken();
	}

	public static DeviceCodeToken getInstanceFromJson(byte[] json)
	{
		try
		{
			return Constants.MAPPER.readValue(json, DeviceCodeToken.class);
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

	public DeviceCodeStatus getDeviceCodeStatus()
	{
		return deviceCodeStatus;
	}

	public void setDeviceCodeStatus(DeviceCodeStatus deviceCodeStatus)
	{
		this.deviceCodeStatus = deviceCodeStatus;
	}

	public String getUserCode()
	{
		return userCode;
	}

	public void setUserCode(String userCode)
	{
		this.userCode = userCode;
	}

	public Instant getLastPolledAt()
	{
		return lastPolledAt;
	}

	public void setLastPolledAt(Instant lastPolledAt)
	{
		this.lastPolledAt = lastPolledAt;
	}

	public int getCurrentPollInterval()
	{
		return currentPollInterval;
	}

	public void setCurrentPollInterval(int currentPollInterval)
	{
		this.currentPollInterval = currentPollInterval;
	}

	public Long getSubjectEntityId()
	{
		return subjectEntityId;
	}

	public void setSubjectEntityId(Long subjectEntityId)
	{
		this.subjectEntityId = subjectEntityId;
	}

	public OAuthToken getOauthToken()
	{
		return oauthToken;
	}

	public void setOauthToken(OAuthToken oauthToken)
	{
		this.oauthToken = oauthToken;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(deviceCodeStatus, userCode, lastPolledAt, currentPollInterval, subjectEntityId,
				oauthToken);
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
		DeviceCodeToken other = (DeviceCodeToken) obj;
		return deviceCodeStatus == other.deviceCodeStatus && Objects.equals(userCode, other.userCode)
				&& Objects.equals(lastPolledAt, other.lastPolledAt)
				&& currentPollInterval == other.currentPollInterval
				&& Objects.equals(subjectEntityId, other.subjectEntityId)
				&& Objects.equals(oauthToken, other.oauthToken);
	}

	@Override
	public String toString()
	{
		return "DeviceCodeToken [deviceCodeStatus=" + deviceCodeStatus + ", userCode=" + userCode
				+ ", lastPolledAt=" + lastPolledAt + ", currentPollInterval=" + currentPollInterval
				+ ", subjectEntityId=" + subjectEntityId + ", oauthToken=" + oauthToken + "]";
	}
}
