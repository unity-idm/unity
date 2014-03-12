/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Identity type which creates a different identifier for each target, which is valid only for a time span of a single
 * login session.
 * <p>
 * The values of transient identities are stored in session attributes, not in the identities table.
 * @author K. Benedyczak
 */
@Component
public class TransientIdentity extends AbstractIdentityTypeProvider
{
	public static final String ID = "transient";
	private static final List<Attribute<?>> empty = Collections.unmodifiableList(new ArrayList<Attribute<?>>(0));
	private ObjectMapper mapper;
	
	@Autowired
	public TransientIdentity(ObjectMapper mapper)
	{
		this.mapper = mapper;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId()
	{
		return ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDefaultDescription()
	{
		return "Transient targeted id";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<AttributeType> getAttributesSupportedForExtraction()
	{
		return Collections.emptySet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validate(String value) throws IllegalIdentityValueException
	{
		if (value != null)
			throw new IllegalIdentityValueException("Only null identity value is allowed "
					+ "for dynamic identity type");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getComparableValue(String from)
	{
		return from;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Attribute<?>> extractAttributes(String from, Map<String, String> toExtract)
	{
		return empty; 
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toPrettyStringNoPrefix(String from)
	{
		return from;
	}

	@Override
	public boolean isDynamic()
	{
		return true;
	}
	
	@Override
	public String toExternalForm(String realm, String target, String inDbValue)
	{
		if (realm == null || target == null || inDbValue == null)
			return null;
		TransientTargetedIdsModel db = new TransientTargetedIdsModel(mapper, inDbValue);
		try
		{
			InvocationContext ctx = InvocationContext.getCurrent();
			LoginSession ls = ctx.getLoginSession();
			if (ls == null)
				return null;
			return db.getIdentity(realm, target, ls.getId());
		} catch (Exception e)
		{
			return null;
		}
	}

	@Override
	public String createNewIdentity(String realm, String target, String inDbValue)
			throws IllegalTypeException
	{
		if (realm == null || target == null)
			throw new IllegalTypeException("Identity can be created only when target is defined");
		TransientTargetedIdsModel db = new TransientTargetedIdsModel(mapper, inDbValue);
		try
		{
			InvocationContext ctx = InvocationContext.getCurrent();
			LoginSession ls = ctx.getLoginSession();
			if (ls == null)
				return null;
			SessionIdentityModel model = new SessionIdentityModel(mapper, ls, UUID.randomUUID().toString());
			db.addIdentity(realm, target, model, ls.getId());
			db.cleanupExpired();
			return db.serialize();
		} catch (Exception e)
		{
			throw new IllegalTypeException("Identity can be created only when login session is defined", e);
		}
	}

	@Override
	public String resetIdentity(String realm, String target, String inDbValue)
			throws IllegalTypeException
	{
		if (inDbValue == null || (realm == null && target == null))
			return null;

		TransientTargetedIdsModel db = new TransientTargetedIdsModel(mapper, inDbValue);
		db.resetIdentities(realm, target);
		db.cleanupExpired();
		return db.serialize();
	}
}






