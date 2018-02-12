/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Identity type definition holding a persistent id. It is associated with each and every entity. 
 * Can not be removed, without removing the whole containing entity.
 * @author K. Benedyczak
 */
@Component
public class PersistentIdentity extends AbstractIdentityTypeProvider
{
	public static final String ID = "persistent";
	
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
		return "Persistent id";
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
	public void validate(String value)
	{
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getComparableValue(String from, String realm, String target)
	{
		return from;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Attribute> extractAttributes(String from, Map<String, String> toExtract)
	{
		return EMPTY_ATTRS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toPrettyStringNoPrefix(IdentityParam from)
	{
		return from.getValue();
	}

	@Override
	public boolean isDynamic()
	{
		return true;
	}
	
	@Override
	public Identity createNewIdentity(String realm, String target, long entityId)
	{
		String value = UUID.randomUUID().toString();
		Identity ret = new Identity(ID, value, entityId, value);
		ret.setRealm(realm);
		ret.setTarget(null);
		return ret;
	}

	@Override
	public boolean isTargeted()
	{
		return false;
	}

	@Override
	public boolean isExpired(Identity representation)
	{
		return false;
	}
	
	@Override
	public String getHumanFriendlyDescription(MessageSource msg)
	{
		return msg.getMessage("PersistentIdentity.description");
	}

	@Override
	public String toHumanFriendlyString(MessageSource msg, IdentityParam from)
	{
		return msg.getMessage("PersistentIdentity.anonymous");
	}
	
	@Override
	public boolean isEmailVerifiable()
	{
		return false;
	}

	@Override
	public String getHumanFriendlyName(MessageSource msg)
	{
		return msg.getMessage("PersistentIdentity.name");
	}
}
