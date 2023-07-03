/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.service;

import io.imunity.fido.identity.FidoUserHandleIdentity;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.identity.UnknownIdentityException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Helper class for interaction with EntityManager.
 *
 * @author R. Ledzinski
 */
@Component
class FidoEntityHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_FIDO, FidoEntityHelper.class);
	static final String NO_ENTITY_MSG = "FidoExc.noEntity";

	private EntityResolver entityResolver;
	private IdentityResolver identityResolver;
	private MessageSource msg;
	private AttributeSupport attributeSupport;

	@Autowired
	FidoEntityHelper(final EntityResolver entityResolver, final IdentityResolver identityResolver,
					 final AttributeSupport attributeSupport, final MessageSource msg)
	{
		this.entityResolver = entityResolver;
		this.identityResolver = identityResolver;
		this.attributeSupport = attributeSupport;
		this.msg = msg;
	}

	String getDisplayName(final Identities identities) throws FidoException
	{
		Optional<String> displayName = Optional.empty();
		try
		{
			displayName = attributeSupport.getAttributeValueByMetadata(identities.getEntityParam(), "/",
					EntityNameMetadataProvider.NAME);
		} catch (EngineException e)
		{
			log.error("Failed to get entity {} display name", identities.getEntityParam(), e);
		}


		return displayName.orElse("Entity [" + getEntityId(identities.getEntityParam()) + "]");
	}

	Optional<String> getUserHandleForUsername(final String username)
	{
		return getIdentitiesByUsername(username).stream()
				.filter(id -> id.getTypeId().equals(FidoUserHandleIdentity.ID))
				.map(Identity::getName)
				.findFirst();
	}

	Optional<String> getUsernameForUserHandle(final String userHandle)
	{
		return Identities.getUsername(getIdentitiesByUserHandle(userHandle));
	}

	FidoUserHandle getOrCreateUserHandle(final Identities identities) {
		return getOrCreateUserHandle(identities, FidoUserHandle.create().asString());
	}

	FidoUserHandle getOrCreateUserHandle(final Identities identities, final String userHandle) throws FidoException
	{
		if (isNull(identities))
			throw new FidoException(msg.getMessage(NO_ENTITY_MSG));

		Optional<String> uh = identities.getUserHandle();
		if (uh.isPresent())
			return FidoUserHandle.fromString(uh.get());

		FidoUserHandle fidoUserHandle = FidoUserHandle.fromString(userHandle);
		try
		{
			identityResolver.insertIdentity(new IdentityParam(FidoUserHandleIdentity.ID, fidoUserHandle.asString()), identities.getEntityParam());
		} catch (EngineException e)
		{
			log.error("Failed to create identity: ", e);
			throw new FidoException(msg.getMessage("FidoExc.internalError"));
		}

		return fidoUserHandle;
	}

	List<Identity> getIdentitiesByUserHandle(final String userHandle)
	{
		try
		{
			return identityResolver.getIdentitiesForEntity(new EntityParam(new IdentityParam(FidoUserHandleIdentity.ID, userHandle)));
		} catch (IllegalIdentityValueException | UnknownIdentityException e)
		{
			return Collections.emptyList();
		}
	}

	List<Identity> getIdentitiesByUsername(final String username)
	{
		try
		{
			List<Identity> ret = identityResolver.getIdentitiesForEntity(new EntityParam(new IdentityParam(UsernameIdentity.ID, username)));
			if (!ret.isEmpty())
				return ret;
		} catch (IllegalIdentityValueException | UnknownIdentityException e)
		{
			// Ignore these exceptions as Username identity is not required - look for email
		}

		try
		{
			return identityResolver.getIdentitiesForEntity(new EntityParam(new IdentityParam(EmailIdentity.ID, username)));
		} catch (IllegalIdentityValueException | UnknownIdentityException e)
		{
			// Neither Username nor Email identity is defined.
			return Collections.emptyList();
		}
	}

	Optional<Identities> resolveUsername(final Long entityId, final String username) throws FidoException
	{
		List<Identity> identities;
		if (nonNull(entityId))
		{
			try
			{
				identities = identityResolver.getIdentitiesForEntity(new EntityParam(entityId));
			} catch (IllegalIdentityValueException e)
			{
				return Optional.empty();
			}
		} else
		{
			identities = getIdentitiesByUsername(username);
		}

		if (identities.isEmpty())
			return Optional.empty();

		try
		{
			return Optional.of(Identities.builder().identities(identities).build());
		} catch (IllegalArgumentException | NoSuchElementException e)
		{
			log.warn("Got exception: ", e);
			return Optional.empty();
		}
	}

	long getEntityId(final EntityParam entityParam)
	{
		try
		{
			return entityResolver.getEntityId(entityParam);
		} catch (IllegalIdentityValueException e)
		{
			throw new FidoException(msg.getMessage(NO_ENTITY_MSG), e);
		}
	}
}
