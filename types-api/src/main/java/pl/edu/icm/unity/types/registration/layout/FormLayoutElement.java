/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.layout;

/**
 * Types of form layout elements.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public enum FormLayoutElement
{
	LOCAL_SIGNUP(true),
	REMOTE_SIGNUP(true),
	REMOTE_SIGNUP_GRID(true),
	CAPTCHA(true),
	REG_CODE(true),
	IDENTITY,
	ATTRIBUTE,
	GROUP,
	CREDENTIAL,
	AGREEMENT,
	COMMENTS,
	CAPTION,
	SEPARATOR;
	
	private boolean isRegistrationOnly;

	FormLayoutElement()
	{
		this.isRegistrationOnly = false;
	}

	FormLayoutElement(boolean isRegistrationOnly)
	{
		this.isRegistrationOnly = isRegistrationOnly;
	}

	public boolean isRegistrationOnly()
	{
		return isRegistrationOnly;
	}

	public void setRegistrationOnly(boolean isRegistrationOnly)
	{
		this.isRegistrationOnly = isRegistrationOnly;
	}
}
