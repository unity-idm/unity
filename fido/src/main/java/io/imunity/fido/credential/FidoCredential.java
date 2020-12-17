/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.credential;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yubico.webauthn.data.AttestationConveyancePreference;
import com.yubico.webauthn.data.UserVerificationRequirement;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

import java.util.HashSet;
import java.util.Set;

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
	private boolean allowSubdomains = false;
	private Set<String> allowedOrigins = new HashSet<>();

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

	public boolean isAllowSubdomains()
	{
		return allowSubdomains;
	}

	public void setAllowSubdomains(Boolean allowSubdomains)
	{
		this.allowSubdomains = allowSubdomains;
	}

	public Set<String> getAllowedOrigins()
	{
		return allowedOrigins;
	}

	public void setAllowedOrigins(Set<String> allowedOrigins)
	{
		this.allowedOrigins = allowedOrigins;
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
