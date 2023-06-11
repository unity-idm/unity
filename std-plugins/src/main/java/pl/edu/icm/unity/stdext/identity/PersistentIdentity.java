/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.entity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.identity.DynamicIdentityTypeDefinition;

import java.util.UUID;

/**
 * Identity type definition holding a persistent id. It is associated with each and every entity. 
 * Can not be removed, without removing the whole containing entity.
 * @author K. Benedyczak
 */
@Component
public class PersistentIdentity extends AbstractIdentityTypeProvider implements DynamicIdentityTypeDefinition
{
	public static final String ID = "persistent";
	
	@Override
	public String getId()
	{
		return ID;
	}

	@Override
	public String getDefaultDescriptionKey()
	{
		return "PersistentIdentity.description";
	}

	@Override
	public void validate(String value)
	{
	}

	@Override
	public String getComparableValue(String from, String realm, String target)
	{
		return from;
	}

	@Override
	public String toPrettyStringNoPrefix(IdentityParam from)
	{
		return from.getValue();
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
