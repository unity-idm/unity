/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Escaper;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.identity.DynamicIdentityTypeDefinition;

import java.util.UUID;

/**
 * Identity type which creates a different but persistent identifier for each target.
 *  
 * @author K. Benedyczak
 */
@Component
public class TargetedPersistentIdentity extends AbstractIdentityTypeProvider implements DynamicIdentityTypeDefinition
{
	static final Logger log = Log.getLogger(Log.U_SERVER_CORE, TargetedPersistentIdentity.class);
	public static final String ID = "targetedPersistent";
	
	@Override
	public String getId()
	{
		return ID;
	}

	@Override
	public String getDefaultDescriptionKey()
	{
		return "TargetedPersistentIdentity.description";
	}

	@Override
	public void validate(String value)
	{
	}

	@Override
	public String toPrettyStringNoPrefix(IdentityParam from)
	{
		return from.getValue();
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






