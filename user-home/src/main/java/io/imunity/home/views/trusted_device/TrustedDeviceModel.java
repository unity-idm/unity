/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.views.trusted_device;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken;

import java.text.SimpleDateFormat;

class TrustedDeviceModel
{
	private final Token token;
	private final RememberMeToken rememberMeToken;
	private final MessageSource msg;

	TrustedDeviceModel(Token token, MessageSource msg)
	{
		this.token = token;
		this.msg = msg;
		this.rememberMeToken = RememberMeToken
				.getInstanceFromJson(token.getContents());
	}

	public String getCreateTime()
	{
		return new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT)
				.format(token.getCreated());
	}

	public String getExpires()
	{
		return new SimpleDateFormat(Constants.SIMPLE_DATE_FORMAT)
				.format(token.getExpires());
	}

	public String getValue()
	{
		return token.getValue();
	}

	public String getIp()
	{
		return rememberMeToken.getMachineDetails().getIp();
	}

	public String getBrowser()
	{
		return rememberMeToken.getMachineDetails().getBrowser();
	}

	public String getOS()
	{
		return rememberMeToken.getMachineDetails().getOs();
	}

	public String getPolicy()
	{
		try
		{
			return msg.getMessage("RememberMePolicy."
					+ rememberMeToken.getRememberMePolicy().toString());
		} catch (Exception e)
		{
			return rememberMeToken.getRememberMePolicy().toString();
		}
	}

	Token getToken()
	{
		return token;
	}

	RememberMeToken getOAuthToken()
	{
		return rememberMeToken;
	}
}
