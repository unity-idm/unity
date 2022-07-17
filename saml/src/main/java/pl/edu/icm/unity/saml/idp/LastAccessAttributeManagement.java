/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;

@Component
public class LastAccessAttributeManagement
{
	private final AttributesManagement attributesManagement;

	public LastAccessAttributeManagement(@Qualifier("insecure") AttributesManagement attributesManagement)
	{
		this.attributesManagement = attributesManagement;
	}

	public void setAttribute(EntityParam entity, String clientId, Instant lastAccess) throws EngineException
	{
		Collection<AttributeExt> attributes = attributesManagement.getAttributes(entity, "/",
				SAMLSystemAttributeTypeProvider.LAST_ACCESS);
		if (attributes.isEmpty())
		{
			setAttribute(entity, Map.of(clientId, lastAccess));
		} else
		{
			AttributeExt attr = attributes.iterator().next();
			Map<String, Instant> value = getAttributeValue(attr);
			value.put(clientId, lastAccess);
			setAttribute(entity, value);
		}
	}

	public Map<String, Instant> getLastAccessByClient() throws EngineException
	{
		LoginSession entity = InvocationContext.getCurrent().getLoginSession();
		EntityParam entityParam = new EntityParam(entity.getEntityId());
		Collection<AttributeExt> attributes = attributesManagement.getAttributes(entityParam, "/",
				SAMLSystemAttributeTypeProvider.LAST_ACCESS);
		if (attributes.isEmpty())
		{
			return new HashMap<>();
		} else
		{
			return getAttributeValue(attributes.iterator().next());
		}
	}
	
	private void setAttribute(EntityParam entity, Map<String, Instant> value) throws EngineException
	{
		try
		{
			attributesManagement.setAttribute(entity, new Attribute(SAMLSystemAttributeTypeProvider.LAST_ACCESS,
					StringAttributeSyntax.ID, "/", List.of(Constants.MAPPER.writeValueAsString(value))));
		} catch (JsonProcessingException e)
		{
			throw new EngineException("Can not save saml last access attribute");
		}
	}
	
	private Map<String, Instant> getAttributeValue(AttributeExt attr) throws EngineException
	{

		try
		{
			return Constants.MAPPER.readValue(attr.getValues().get(0), new TypeReference<Map<String, Instant>>()
			{
			});
		} catch (Exception e)
		{
			throw new EngineException("Can not save saml last access attribute");
		}

	}
}
