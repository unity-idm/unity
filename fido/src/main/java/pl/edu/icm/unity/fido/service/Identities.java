/******************************************************************************
 * Copyright (c) 2020, T-Mobile US.
 * <p>
 * All Rights Reserved
 * <p>
 * This is unpublished proprietary source code of T-Mobile US.
 * <p>
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *******************************************************************************/
package pl.edu.icm.unity.fido.service;

import pl.edu.icm.unity.fido.identity.FidoUserHandleIdentity;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

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

	String getUsername()
	{
		Optional<String> name = identities.stream()
				.filter(id -> id.getTypeId().equals(UsernameIdentity.ID))
				.findFirst()
				.map(Identity::getName);
		return name.orElseGet(() -> identities.stream()
				.filter(id -> id.getTypeId().equals(EmailIdentity.ID))
				.map(Identity::getName)
				.findFirst()
				.get());
	}

	Optional<String> getUserHandle()
	{
		return identities.stream()
				.filter(id -> id.getTypeId().equals(FidoUserHandleIdentity.ID))
				.map(Identity::getName)
				.findAny();
	}

	EntityParam getEntityParam()
	{
		Optional<Identity> identity = identities.stream()
				.filter(id -> id.getTypeId().equals(UsernameIdentity.ID))
				.findFirst();
		if (!identity.isPresent())
		{
			identity = identities.stream()
					.filter(id -> id.getTypeId().equals(EmailIdentity.ID))
					.findFirst();
			return new EntityParam(new IdentityParam(EmailIdentity.ID, identity.get().getValue()));
		} else
		{
			return new EntityParam(new IdentityParam(UsernameIdentity.ID, identity.get().getValue()));
		}
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
			Identities ret = new Identities(identities);
			if (ret.getUsername().isEmpty())
				throw new IllegalArgumentException("Cannot retrieve username value for given identities.");
			return ret;
		}
	}
}
