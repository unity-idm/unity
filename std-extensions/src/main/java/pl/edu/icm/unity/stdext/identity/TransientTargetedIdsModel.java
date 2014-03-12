/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Model of DB data for {@link TransientIdentity}. Each targeted value is modeled as {@link SessionIdentityModel},
 * i.e. for each session there is a separate identity value.
 * @author K. Benedyczak
 */
public class TransientTargetedIdsModel extends AbstractTargetedIdsDbModel<SessionIdentityModel>
{
	public TransientTargetedIdsModel(ObjectMapper mapper, String value)
	{
		super(mapper, value);
	}

	@Override
	protected SessionIdentityModel deserializeValue(JsonNode node)
	{
		return new SessionIdentityModel(mapper, node);
	}

	@Override
	protected JsonNode serializeValue(SessionIdentityModel value)
	{
		return value.serialize();
	}
	
	public String getIdentity(String realm, String target, String sessionId)
	{
		SessionIdentityModel sessionIdModel = getIdentity(realm, target);
		if (sessionIdModel == null)
			return null;
		return sessionIdModel.getIdentity(sessionId);
	}
	
	public void addIdentity(String realm, String target, SessionIdentityModel value, String sessionId)
	{
		Map<String, SessionIdentityModel> realmMap = realmsMap.get(realm);
		if (realmMap == null)
		{
			realmMap = new HashMap<>();
			realmsMap.put(realm, realmMap);
			realmMap.put(target, value);
			return;
		}
		SessionIdentityModel current = realmMap.get(target);
		if (current == null)
		{
			realmMap.put(target, value);
			return;
		}
		current.merge(value);
	}
	
	/**
	 * Iterates over all targets. All outdated entries are removed.
	 * An entry is outdated when simultaneously current time is after its absoluteTTL and  
	 */
	public void cleanupExpired()
	{
		long now = System.currentTimeMillis();
		for (Map.Entry<String, Map<String, SessionIdentityModel>> e: realmsMap.entrySet())
		{
			Iterator<Map.Entry<String, SessionIdentityModel>> it = e.getValue().entrySet().iterator();
			while (it.hasNext())
			{
				SessionIdentityModel model = it.next().getValue();
				model.cleanup(now);
				if (model.isEmpty())
					it.remove();
			}
		}
	}
}
