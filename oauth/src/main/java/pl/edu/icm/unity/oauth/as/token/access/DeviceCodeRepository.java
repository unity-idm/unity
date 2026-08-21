/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.device.UserCode;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.exceptions.IllegalTypeException;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.DeviceCodeToken;
import pl.edu.icm.unity.store.api.TokenDAO.TokenNotFoundException;

/**
 * Built on top of generic token storage, handles access to persisted RFC 8628 device codes.
 */
@Component
public class DeviceCodeRepository
{
	public static final String INTERNAL_DEVICE_TOKEN = "oauth2Device";

	private final TokensManagement tokensMan;

	@Autowired
	public DeviceCodeRepository(TokensManagement tokensMan)
	{
		this.tokensMan = tokensMan;
	}

	public void store(String deviceCode, DeviceCodeToken token, Date now, Date expiration)
			throws IllegalTypeException, JsonProcessingException
	{
		tokensMan.addToken(INTERNAL_DEVICE_TOKEN, deviceCode, token.getSerialized(), now, expiration);
	}

	public Optional<Token> getByDeviceCode(String deviceCode)
	{
		try
		{
			return Optional.of(tokensMan.getTokenById(INTERNAL_DEVICE_TOKEN, deviceCode));
		} catch (TokenNotFoundException e)
		{
			return Optional.empty();
		}
	}

	public Optional<Token> findByUserCode(String userCode)
	{
		String normalized = new UserCode(userCode).getStrippedValue();
		List<Token> all = tokensMan.getAllTokens(INTERNAL_DEVICE_TOKEN);
		for (Token token : all)
		{
			DeviceCodeToken deviceCodeToken = DeviceCodeToken.getInstanceFromJson(token.getContents());
			if (deviceCodeToken.getUserCode() != null
					&& new UserCode(deviceCodeToken.getUserCode()).getStrippedValue().equals(normalized))
				return Optional.of(token);
		}
		return Optional.empty();
	}

	public void update(String deviceCode, DeviceCodeToken token, Date expires) throws JsonProcessingException
	{
		tokensMan.updateToken(INTERNAL_DEVICE_TOKEN, deviceCode, expires, token.getSerialized());
	}

	public void remove(String deviceCode)
	{
		tokensMan.removeToken(INTERNAL_DEVICE_TOKEN, deviceCode);
	}
}
