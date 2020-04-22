/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.fido;

import pl.edu.icm.unity.types.authn.FidoCredentialInfo;

import java.util.Date;

/**
 * Wrapper over {@link FidoCredentialInfo}. Keeps also persistence state for UI needs.
 *
 * @author R. Ledzinski
 */
public class FidoCredentialInfoWrapper
{
	private CredentialState state;

	public FidoCredentialInfo getCredential()
	{
		return credential;
	}

	private FidoCredentialInfo credential;

	public FidoCredentialInfoWrapper(final CredentialState state, final FidoCredentialInfo credential)
	{
		this.state = state;
		this.credential = credential;
	}

	public CredentialState getState()
	{
		return state;
	}

	public void setState(final CredentialState state)
	{
		this.state = state;
	}

	public Date getRegistrationTimestamp()
	{
		return new Date(credential.getRegistrationTimestamp());
	}

	enum CredentialState
	{
		STORED,
		NEW,
		DELETED
	}

	;
}
