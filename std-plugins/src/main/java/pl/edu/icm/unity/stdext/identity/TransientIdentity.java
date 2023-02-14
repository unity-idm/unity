/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Escaper;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.identity.DynamicIdentityTypeDefinition;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.identity.SessionIdentityModel.PerSessionEntry;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

import java.util.UUID;

/**
 * Identity type which creates a different identifier for each target, which is valid only for a time span of a single
 * login session.
 * @author K. Benedyczak
 */
@Component
public class TransientIdentity extends AbstractIdentityTypeProvider implements DynamicIdentityTypeDefinition
{
	public static final String ID = "transient";
	private ObjectMapper mapper = Constants.MAPPER;
	
	@Override
	public String getId()
	{
		return ID;
	}

	@Override
	public String getDefaultDescriptionKey()
	{
		return "TransientIdentity.description";
	}
	
	@Override
	public boolean isRemovable()
	{
		return false;
	}
	
	@Override
	public void validate(String value)
	{
	}

	@Override
	public String getComparableValue(String from, String realm, String target)
	{
		if (realm == null || target == null)
			return null;
		LoginSession ls;
		try
		{
			InvocationContext ctx = InvocationContext.getCurrent();
			ls = ctx.getLoginSession();
			if (ls == null)
				return null;
		} catch (InternalException e)
		{
			return null;
		}
		
		return getComparableValueInternal(from, realm, target, ls);
	}

	private String getComparableValueInternal(String from, String realm, String target, LoginSession ls)
	{
		return Escaper.encode(realm, target, ls.getId(), from);
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
			throw new IllegalArgumentException("Identity can be created only when target is defined");
		String value = UUID.randomUUID().toString();
		try
		{
			InvocationContext ctx = InvocationContext.getCurrent();
			LoginSession ls = ctx.getLoginSession();
			if (ls == null)
				return null;
			
			SessionIdentityModel model = new SessionIdentityModel(mapper, ls, value);
			
			ObjectNode contents = model.serialize();
			String comparableValue = getComparableValueInternal(value, realm, target, ls);
			Identity ret = new Identity(ID, value, entityId, comparableValue);
			ret.setMetadata(contents);
			ret.setTarget(target);
			ret.setRealm(realm);
			return ret;
		} catch (Exception e)
		{
			throw new IllegalStateException("Identity can be created only when "
					+ "login session is defined", e);
		}
	}

	@Override
	public boolean isExpired(Identity representation)
	{
		
		SessionIdentityModel model = new SessionIdentityModel(mapper, 
				(ObjectNode) representation.getMetadata());
		PerSessionEntry info = model.getEntry();
		return info.isExpired();
	}

	@Override
	public boolean isTargeted()
	{
		return true;
	}

	@Override
	public String toHumanFriendlyString(MessageSource msg, IdentityParam from)
	{
		return msg.getMessage("TransientIdentity.random");
	}

	@Override
	public String getHumanFriendlyDescription(MessageSource msg)
	{
		return msg.getMessage("TransientIdentity.description");
	}
	
	@Override
	public boolean isEmailVerifiable()
	{
		return false;
	}

	@Override
	public String getHumanFriendlyName(MessageSource msg)
	{
		return msg.getMessage("TransientIdentity.name");
	}
}






