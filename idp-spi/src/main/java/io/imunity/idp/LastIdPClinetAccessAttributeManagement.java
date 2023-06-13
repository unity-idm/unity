/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.idp;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

@Component
public class LastIdPClinetAccessAttributeManagement
{
	private final AttributesManagement attributesManagement;

	public LastIdPClinetAccessAttributeManagement(@Qualifier("insecure") AttributesManagement attributesManagement)
	{
		this.attributesManagement = attributesManagement;
	}

	public synchronized void setAttribute(EntityParam entity, AccessProtocol accessProtocol, ApplicationId clientId,
			Instant lastAccess) throws EngineException
	{
		Collection<AttributeExt> attributes = attributesManagement.getAttributes(entity, "/",
				IdPSystemAttributeTypeProvider.LAST_ACCESS);
		if (attributes.isEmpty())
		{
			setAttribute(entity, Map.of(new LastIdPClientAccessKey(accessProtocol, clientId.id), lastAccess));
		} else
		{
			AttributeExt attr = attributes.iterator().next();
			Map<LastIdPClientAccessKey, Instant> value = getAttributeValue(attr);
			value.put(new LastIdPClientAccessKey(accessProtocol, clientId.id), lastAccess);
			setAttribute(entity, value);
		}
	}

	public Map<LastIdPClientAccessKey, Instant> getLastAccessByClient() throws EngineException
	{
		LoginSession entity = InvocationContext.getCurrent().getLoginSession();
		EntityParam entityParam = new EntityParam(entity.getEntityId());
		Collection<AttributeExt> attributes = attributesManagement.getAttributes(entityParam, "/",
				IdPSystemAttributeTypeProvider.LAST_ACCESS);
		if (attributes.isEmpty())
		{
			return new HashMap<>();
		} else
		{
			return getAttributeValue(attributes.iterator().next());
		}
	}

	private void setAttribute(EntityParam entity, Map<LastIdPClientAccessKey, Instant> value) throws EngineException
	{
		try
		{
			Map<String, Instant> toSave = new HashMap<>();
			for (Entry<LastIdPClientAccessKey, Instant> entry : value.entrySet())
			{
				toSave.put(Constants.MAPPER.writeValueAsString(entry.getKey()), entry.getValue());	
			}
			attributesManagement.setAttribute(entity, new Attribute(IdPSystemAttributeTypeProvider.LAST_ACCESS,
					StringAttributeSyntax.ID, "/",
					List.of(Constants.MAPPER.writeValueAsString(toSave))));
		} catch (JsonProcessingException e)
		{
			throw new EngineException("Can not save idP last access attribute", e);
		}
	}

	private Map<LastIdPClientAccessKey, Instant> getAttributeValue(AttributeExt attr) throws EngineException
	{

		try
		{
			return Constants.MAPPER.readValue(attr.getValues().get(0),
					new TypeReference<Map<LastIdPClientAccessKey, Instant>>()
					{
					});
		} catch (Exception e)
		{
			throw new EngineException("Can not get idP last access attribute", e);
		}
	}

	public static class LastIdPClientAccessKey
	{
		public final AccessProtocol accessProtocol;
		public final String clientId;

		@JsonCreator
		public LastIdPClientAccessKey(@JsonProperty("accessProtocol") AccessProtocol accessProtocol,
				@JsonProperty("clientId") String clientId)
		{
			this.accessProtocol = accessProtocol;
			this.clientId = clientId;
		}

		@JsonCreator
		public LastIdPClientAccessKey(String from) throws JsonMappingException, JsonProcessingException
		{
			LastIdPClientAccessKey readValue = Constants.MAPPER.readValue(from,
					new TypeReference<LastIdPClientAccessKey>()
					{
					});
			this.accessProtocol = readValue.accessProtocol;
			this.clientId = readValue.clientId;

		}

		@Override
		public int hashCode()
		{
			return Objects.hash(accessProtocol, clientId);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LastIdPClientAccessKey other = (LastIdPClientAccessKey) obj;
			return accessProtocol == other.accessProtocol && Objects.equals(clientId, other.clientId);
		}

	}

}
