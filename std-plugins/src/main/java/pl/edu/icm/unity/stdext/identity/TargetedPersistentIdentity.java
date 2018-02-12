/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Escaper;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Identity type which creates a different but persistent identifier for each target.
 *  
 * @author K. Benedyczak
 */
@Component
public class TargetedPersistentIdentity extends AbstractIdentityTypeProvider
{
	static final Logger log = Log.getLogger(Log.U_SERVER, TargetedPersistentIdentity.class);
	public static final String ID = "targetedPersistent";
	
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
		return "Targeted persistent id";
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
		if (realm == null || target == null)
			throw new IllegalArgumentException("Targeted persistent identity can be created only "
					+ "when target is defined");
		String inDbValue = UUID.randomUUID().toString();
		String cmpVal = Escaper.encode(realm, target, inDbValue);
		Identity ret = new Identity(ID, inDbValue, entityId, cmpVal);
		ret.setRealm(realm);
		ret.setTarget(target);
		return ret;
	}

	@Override
	public boolean isTargeted()
	{
		return true;
	}

	@Override
	public String getComparableValue(String from, String realm, String target)
			throws IllegalIdentityValueException
	{
		return Escaper.encode(realm, target, from);
	}

	@Override
	public boolean isExpired(Identity representation)
	{
		return false;
	}
	
	@Override
	public String getHumanFriendlyDescription(MessageSource msg)
	{
		return msg.getMessage("TargetedPersistentIdentity.description");
	}
	

	@Override
	public String toHumanFriendlyString(MessageSource msg, IdentityParam from)
	{
		return msg.getMessage("TargetedPersistentIdentity.fullyAnonymous");
	}
	
	@Override
	public boolean isEmailVerifiable()
	{
		return false;
	}

	@Override
	public String getHumanFriendlyName(MessageSource msg)
	{
		return msg.getMessage("TargetedPersistentIdentity.name");
	}
}






