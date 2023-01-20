/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.web.v8;

import io.imunity.fido.credential.FidoCredentialInfo;

import java.util.Date;

/**
 * Wrapper over {@link FidoCredentialInfo}. Keeps also persistence state for UI needs.
 *
 * @author R. Ledzinski
 */
class FidoCredentialInfoWrapper
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

	public void setDescription(String description)
	{
		credential = credential.copyBuilder()
				.description(description)
				.build();
	}

	public boolean isDeleted()
	{
		return CredentialState.DELETED == state;
	}

	enum CredentialState
	{
		STORED,
		NEW,
		DELETED
	};
}
