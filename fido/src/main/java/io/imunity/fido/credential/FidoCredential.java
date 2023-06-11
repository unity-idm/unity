/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.credential;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yubico.webauthn.data.AttestationConveyancePreference;
import com.yubico.webauthn.data.UserVerificationRequirement;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.InternalException;

/**
 * Represents business object of fido credential definition.
 *
 * @author R. Ledzinski
 */
public class FidoCredential
{
	private String attestationConveyance = AttestationConveyancePreference.DIRECT.toString();
	private String userVerification = UserVerificationRequirement.REQUIRED.toString();
	private String hostName = "Unity";
	private boolean loginLessAllowed;

	public String getAttestationConveyance()
	{
		return attestationConveyance;
	}

	public void setAttestationConveyance(String attestationConveyance)
	{
		this.attestationConveyance = attestationConveyance;
	}

	public String getUserVerification()
	{
		return userVerification;
	}

	public void setUserVerification(String userVerification)
	{
		this.userVerification = userVerification;
	}

	public String getHostName()
	{
		return hostName;
	}

	public void setHostName(String hostName)
	{
		this.hostName = hostName;
	}

	public boolean isLoginLessAllowed()
	{
		return loginLessAllowed;
	}

	public void setLoginLessOption(boolean loginLessAllowed)
	{
		this.loginLessAllowed = loginLessAllowed;
	}

	public String serialize()
	{
		try
		{
			return Constants.MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}

	public static FidoCredential deserialize(final String credentialDefinitionConfiguration)
	{
		try
		{
			return Constants.MAPPER.readValue(credentialDefinitionConfiguration, FidoCredential.class);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}
}
