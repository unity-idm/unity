/******************************************************************************
 * Copyright (c) 2020, T-Mobile US.
 * <p>
 * All Rights Reserved
 * <p>
 * This is unpublished proprietary source code of T-Mobile US.
 * <p>
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *******************************************************************************/
package io.imunity.fido.service;

import io.imunity.fido.identity.FidoUserHandleIdentity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.api.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

import java.util.List;
import java.util.Optional;

/**
 * Keeps all {@link Identity} object related to one {@link Entity} together.
 * It is guaranteed that getUsername() returns meaningful value.
 */
class Identities
{
	private final List<Identity> identities;

	private Identities(final List<Identity> identities)
	{
		this.identities = identities;
	}

	String getUsername() {
		return getUsername(identities)
				.orElseThrow(() -> new RuntimeEngineException("Invalid state of Identities object - unknown username."));
	}

	static Optional<String> getUsername(List<Identity> identities)
	{
		return getUsernameIdentity(identities).map(Identity::getName);
	}

	static private Optional<Identity> getUsernameIdentity(List<Identity> identities)
	{
		return identities.stream()
				.filter(id -> id.getTypeId().equals(UsernameIdentity.ID) || id.getTypeId().equals(EmailIdentity.ID))
				.reduce((x, y) -> x.getTypeId().equals(UsernameIdentity.ID) ? x : y);
	}

	Optional<String> getUserHandle()
	{
		return identities.stream()
				.filter(id -> id.getTypeId().equals(FidoUserHandleIdentity.ID))
				.map(Identity::getValue)
				.findAny();
	}

	EntityParam getEntityParam()
	{
		return getUsernameIdentity(identities)
				.map(id -> new EntityParam(new IdentityParam(id.getTypeId(),id.getValue())))
				.orElseThrow(() -> new RuntimeEngineException("Invalid state of Identities object - unknown username."));
	}

	static IdentitiesBuilder builder()
	{
		return new IdentitiesBuilder();
	}

	public static final class IdentitiesBuilder
	{
		private List<Identity> identities;

		private IdentitiesBuilder()
		{
		}

		public IdentitiesBuilder identities(List<Identity> identities)
		{
			this.identities = identities;
			return this;
		}

		public Identities build()
		{
			return getUsername(identities)
					.map(v -> new Identities(identities))
					.orElseThrow(() -> new IllegalArgumentException("Cannot retrieve username value for given identities."));
		}
	}
}
