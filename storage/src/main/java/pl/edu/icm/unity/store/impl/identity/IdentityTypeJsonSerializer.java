/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identity;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.base.utils.JsonSerializer;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.store.rdbms.model.BaseBean;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Handles serialization of {@link IdentityType} metadata. The metadata
 * is common for all identity types.
 * @author K. Benedyczak
 */
@Component
public class IdentityTypeJsonSerializer implements RDBMSObjectSerializer<IdentityType, BaseBean>,
	JsonSerializer<IdentityType>
{
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private IdentityTypesRegistry idTypesRegistry;
	
	@Override
	public IdentityType fromDB(BaseBean raw)
	{
		IdentityType it = new IdentityType(idTypesRegistry.getByName(raw.getName()));
		fromJson(raw.getContents(), it);
		return it;
	}
	
	@Override
	public BaseBean toDB(IdentityType idType)
	{
		BaseBean toAdd = new BaseBean();

		if (idType.getDescription() == null)
			idType.setDescription(idType.getIdentityTypeProvider().getDefaultDescription());
		toAdd.setName(idType.getIdentityTypeProvider().getId());
		toAdd.setContents(JsonUtil.serialize2Bytes(toJsonBase(idType)));
		return toAdd;
	}

	@Override
	public IdentityType fromJson(ObjectNode main)
	{
		IdentityType it = new IdentityType(idTypesRegistry.getByName(
				main.get("name").asText()));
		fromJson(main, it);
		return it;
	}
	
	@Override
	public ObjectNode toJson(IdentityType src)
	{
		ObjectNode main = toJsonBase(src);
		main.put("name", src.getIdentityTypeProvider().getId());
		return main;
	}

	private ObjectNode toJsonBase(IdentityType src)
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("description", src.getDescription());
		main.put("selfModificable", src.isSelfModificable());
		main.put("minInstances", src.getMinInstances());
		main.put("maxInstances", src.getMaxInstances());
		main.put("minVerifiedInstances", src.getMinVerifiedInstances());
		ArrayNode extractedA = main.putArray("extractedAttributes");
		for (Map.Entry<String, String> a: src.getExtractedAttributes().entrySet())
		{
			ObjectNode entry = mapper.createObjectNode();
			entry.put("key", a.getKey());
			entry.put("value", a.getValue());
			extractedA.add(entry);
		}
		return main;
	}
	
	/**
	 * Fills target with JSON contents, checking it for correctness
	 * @param json
	 * @param target
	 */
	private void fromJson(byte[] json, IdentityType target)
	{
		if (json == null)
			return;
		ObjectNode main = JsonUtil.parse(json);
		fromJson(main, target);
	}

	private void fromJson(ObjectNode main, IdentityType target)
	{
		target.setDescription(main.get("description").asText());
		ArrayNode attrs = main.withArray("extractedAttributes");
		Map<String, String> attrs2 = new HashMap<String, String>();
		for (JsonNode a: attrs)
		{
			attrs2.put(a.get("key").asText(), a.get("value").asText());
		}
		target.setExtractedAttributes(attrs2);
		
		if (main.has("selfModificable"))
			target.setSelfModificable(main.get("selfModificable").asBoolean());
		else
			target.setSelfModificable(false);
		
		if (main.has("minInstances"))
		{
			target.setMinInstances(main.get("minInstances").asInt());
			target.setMinVerifiedInstances(main.get("minVerifiedInstances").asInt());
			target.setMaxInstances(main.get("maxInstances").asInt());
		} else
		{
			target.setMinInstances(0);
			target.setMinVerifiedInstances(0);
			target.setMaxInstances(Integer.MAX_VALUE);
		}
	}
}



