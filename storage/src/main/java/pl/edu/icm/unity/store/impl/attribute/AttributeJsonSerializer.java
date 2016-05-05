/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.StoredAttribute;
import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * Serializes {@link Attribute} to/from JSON.
 * @author K. Benedyczak
 */
@Component
public class AttributeJsonSerializer implements JsonSerializerForKryo<StoredAttribute>
{
	@Autowired
	private ObjectMapper mapper;
	@Autowired
	private AttributeTypeDAO atDAO;
	
	@SuppressWarnings("unchecked")
	@Override
	public StoredAttribute fromJson(ObjectNode main)
	{
		if (main == null)
			return null;
		@SuppressWarnings("rawtypes")
		AttributeExt ret = new AttributeExt();

		fillCommon(main, ret);
		fromJsonBaseExt(main, ret);

		ret.setDirect(true);
		long entityId = main.get("entityId").asLong();
		return new StoredAttribute(ret, entityId);
	}
	
	@SuppressWarnings("unchecked")
	public Attribute<?> fromJsonBasic(ObjectNode main)
	{
		if (main == null)
			return null;
		@SuppressWarnings("rawtypes")
		Attribute ret = new Attribute();
		fillCommon(main, ret);
		fromJsonBase(main, ret);
		return ret;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void fillCommon(ObjectNode main, Attribute ret)
	{
		String name = main.get("name").asText();
		ret.setName(name);
		AttributeType type = atDAO.get(name);
		ret.setAttributeSyntax(type.getValueType());
		ret.setGroupPath(main.get("groupPath").asText());
	}
	
	@Override
	public ObjectNode toJson(StoredAttribute src)
	{
		ObjectNode root = toJsonBaseExt(src.getAttribute());
		storeCommon(root, src.getAttribute());
		root.put("entityId", src.getEntityId());
		return root;
	}
	
	public ObjectNode toJsonBasic(Attribute<?> src)
	{
		ObjectNode root = toJsonBase(src);
		storeCommon(root, src);
		return root;
	}
	
	private void storeCommon(ObjectNode root, Attribute<?> src)
	{
		root.put("name", src.getName());
		root.put("groupPath", src.getGroupPath());
	}
	
	protected <T> ObjectNode toJsonBaseExt(AttributeExt<T> src)
	{
		ObjectNode root = toJsonBase(src);
		if (src.getCreationTs() != null)
			root.put("creationTs", src.getCreationTs().getTime());
		if (src.getUpdateTs() != null)
			root.put("updateTs", src.getUpdateTs().getTime());
		return root;
	}
	
	protected <T> ObjectNode toJsonBase(Attribute<T> src)
	{
		ObjectNode root = mapper.createObjectNode();
		root.put("visibility", src.getVisibility().name());
		if (src.getRemoteIdp() != null)
			root.put("remoteIdp", src.getRemoteIdp());
		if (src.getTranslationProfile() != null)
			root.put("translationProfile", src.getTranslationProfile());
		ArrayNode values = root.putArray("values");
		AttributeValueSyntax<T> syntax = src.getAttributeSyntax();
		for (T value: src.getValues())
			values.add(syntax.serialize(value));
		return root;
	}
	
	protected <T> void fromJsonBaseExt(ObjectNode main, AttributeExt<T> target)
	{
		fromJsonBase(main, target);
		
		if (main.has("creationTs"))
			target.setCreationTs(new Date(main.get("creationTs").asLong()));
		if (main.has("updateTs"))
			target.setUpdateTs(new Date(main.get("updateTs").asLong()));
	}

	protected <T> void fromJsonBase(ObjectNode main, Attribute<T> target)
	{
		target.setVisibility(AttributeVisibility.valueOf(main.get("visibility").asText()));
		
		if (main.has("translationProfile"))
			target.setTranslationProfile(main.get("translationProfile").asText());
		if (main.has("remoteIdp"))
			target.setRemoteIdp(main.get("remoteIdp").asText());
		ArrayNode values = main.withArray("values");
		List<T> pValues = new ArrayList<T>(values.size());
		Iterator<JsonNode> it = values.iterator();
		AttributeValueSyntax<T> syntax = target.getAttributeSyntax();
		try
		{
			while(it.hasNext())
				pValues.add(syntax.deserialize(it.next().binaryValue()));
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
		target.setValues(pValues);
	}

	@Override
	public Class<StoredAttribute> getClazz()
	{
		return StoredAttribute.class;
	}
}
