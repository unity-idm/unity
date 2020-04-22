/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.fido;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.FidoException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UserHandleIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.UserHandle;

import javax.swing.text.html.Option;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Helper class for interaction with EntityManager.
 *
 * @author R. Ledzinski
 */
@Component
public class FidoEntityHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, FidoEntityHelper.class);

	private EntityManagement entityMan;
	private UnityMessageSource msg;

	@Autowired
	FidoEntityHelper(final EntityManagement entityMan, final UnityMessageSource msg)
	{
		this.entityMan = entityMan;
		this.msg = msg;
	}

	String getDisplayName(final Entity entity) throws FidoException
	{
		if (isNull(entity))
			throw new FidoException(msg.getMessage("FidoExc.noEntity"));

		String displayName = null;
		try
		{
			displayName = entityMan.getEntityLabel(new EntityParam(entity.getId()));
		} catch (EngineException e)
		{
			// ignore errors
		} finally
		{
			if (isNull(displayName))
			{
				displayName = "Entity [" + entity.getId() + "]";
			}
		}
		return displayName;
	}

	String getUsername(final Entity entity) throws FidoException
	{
		if (isNull(entity))
			throw new FidoException(msg.getMessage("FidoExc.noEntity"));

		String entityUsername = entity.getIdentities().stream()
				.filter(id -> id.getTypeId().equals(UsernameIdentity.ID))
				.map(Identity::getName)
				.findAny()
				.orElse(null);

		if (isNull(entityUsername))
		{
			entityUsername = entity.getIdentities().stream()
					.filter(id -> id.getTypeId().equals(EmailIdentity.ID))
					.map(Identity::getName)
					.findAny()
					.orElseThrow(() -> new FidoException(msg.getMessage("FidoExc.noUsername")));
		}
		return entityUsername;
	}

	Optional<String> getUserHandleForUsername(final String username)
	{
		try
		{
			return Optional.of(getUserHandle(getEntityByUsername(username)));
		} catch (FidoException e)
		{
			log.error("Got exception: ", e);
			return Optional.empty();
		}
	}

	Optional<String> getUsernameForUserHandle(final String userHandle)
	{
		try
		{
			return Optional.of(getUsername(getEntityByUserHandle(userHandle)));
		} catch (FidoException e)
		{
			log.error("Got exception: ", e);
			return Optional.empty();
		}
	}

	String getOrCreateUserHandle(final Entity entity) throws FidoException
	{
		if (isNull(entity))
			throw new FidoException(msg.getMessage("FidoExc.noEntity"));

		Optional<String> uh = entity.getIdentities().stream()
				.filter(id -> id.getTypeId().equals(UserHandleIdentity.ID))
				.map(Identity::getName)
				.findAny();
		if (uh.isPresent())
			return uh.get();

		Identity userHandleIdentity = null;
		try
		{
			IdentityParam userHandle = new IdentityParam(UserHandleIdentity.ID, UserHandle.create().asString());
			userHandleIdentity = entityMan.addIdentity(userHandle, new EntityParam(entity.getId()));
			entity.getIdentities().add(userHandleIdentity);
		} catch (EngineException e)
		{
			log.error("Failed to create identity: ", e);
			throw new FidoException(msg.getMessage("FidoExc.internalError"));
		}

		return userHandleIdentity.getValue();
	}

	String getUserHandle(final Entity entity) throws FidoException
	{
		if (isNull(entity))
			throw new FidoException(msg.getMessage("FidoExc.noEntity"));

		return entity.getIdentities().stream()
				.filter(id -> id.getTypeId().equals(UserHandleIdentity.ID))
				.map(Identity::getName)
				.findAny()
				.orElseThrow(() -> new FidoException(msg.getMessage("FidoExc.noFidoCredential")));
	}

	Entity getEntityByUserHandle(final String userHandle)
	{
		return getEntity(new EntityParam(new IdentityParam(UserHandleIdentity.ID, userHandle)));
	}

	Entity getEntityByUsername(final String username)
	{
		return Optional
				.ofNullable(getEntity(new EntityParam(new IdentityParam(UsernameIdentity.ID, username))))
				.orElse(getEntity(new EntityParam(new IdentityParam(EmailIdentity.ID, username))));
	}

	Entity getEntityOrThrow(final Long entityId, final String username) throws FidoException
	{
		Entity entity = getEntity(entityId, username);
		if (isNull(entity))
		{
			if (isNull(entityId))
				throw new FidoException(msg.getMessage("FidoExc.noEntityForName"));
			else
				throw new FidoException(msg.getMessage("FidoExc.noEntity"));
		}
		return entity;
	}

	private Entity getEntity(final Long entityId, final String username)
	{
		if (nonNull(entityId))
		{
			return getEntity(new EntityParam(entityId));
		} else
		{
			return getEntityByUsername(username);
		}
	}

	private Entity getEntity(final EntityParam entityParam)
	{
		try
		{
			return entityMan.getEntity(entityParam);
		} catch (Exception e)
		{
			return null;
		}
	}
}
