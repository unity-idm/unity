/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.layout;

/**
 * Types of form layout elements.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public enum FormLayoutType
{
	LOCAL_SIGNUP(true),
	REMOTE_SIGNUP(true),
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

	FormLayoutType()
	{
		this.isRegistrationOnly = false;
	}

	FormLayoutType(boolean isRegistrationOnly)
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
