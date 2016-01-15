/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.mapper.AttributesMapper;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.AttributeBean;
import pl.edu.icm.unity.db.model.AttributeTypeBean;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.resolvers.AttributesResolver;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.attributes.AttributeValueChecker;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement2;
import pl.edu.icm.unity.types.basic.AttributeStatement2.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Group;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles serialization of Groups metadata.
 * @author K. Benedyczak
 */
@Component
public class GroupsSerializer
{
	private ObjectMapper mapper;
	private AttributeSerializer attributeSerializer;
	private AttributesResolver attributeResolver;
	private GroupResolver groupResolver;

	@Autowired
	public GroupsSerializer(ObjectMapper mapper,
			AttributeSerializer attributeSerializer,
			AttributesResolver attributeResolver, GroupResolver groupResolver)
	{
		this.mapper = mapper;
		this.attributeSerializer = attributeSerializer;
		this.attributeResolver = attributeResolver;
		this.groupResolver = groupResolver;
	}

	/**
	 * @param src
	 * @return Json as byte[] with the src contents.
	 * @throws IllegalAttributeTypeException 
	 * @throws IllegalGroupValueException 
	 */
	public byte[] toJson(Group src, GroupsMapper groupMapper, 
			AttributesMapper attributeMapper) 
			throws IllegalGroupValueException, IllegalAttributeTypeException
	{
		try
		{
			ObjectNode main = mapper.createObjectNode();
			main.set("i18nDescription", I18nStringJsonUtil.toJson(src.getDescription()));
			main.set("displayedName", I18nStringJsonUtil.toJson(src.getDisplayedName()));
			ArrayNode ases = main.putArray("attributeStatements");
			for (AttributeStatement2 as: src.getAttributeStatements())
			{
				ases.add(serializeAS(as, groupMapper, attributeMapper));
			}
			ArrayNode aces = main.putArray("attributesClasses");
			for (String ac: src.getAttributesClasses())
			{
				aces.add(ac);
			}
			
			return mapper.writeValueAsBytes(main);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}
	
	/**
	 * Fills target with JSON contents, checking it for correctness.
	 * If mappers are not provided (null) then the costly operation of group 
	 * attribute statements resolving is not performed.
	 * @param json
	 * @param target
	 */
	public int fromJson(byte[] json, Group target, GroupsMapper groupMapper, 
			AttributesMapper attributeMapper)
	{
		if (json == null)
			return 0;
		ObjectNode main;
		int outdatedASes = 0;
		try
		{
			main = mapper.readValue(json, ObjectNode.class);
			target.setDescription(I18nStringJsonUtil.fromJson(main.get("i18nDescription"),
					main.get("description")));
			target.setDisplayedName(main.has("displayedName") ? 
					I18nStringJsonUtil.fromJson(main.get("displayedName")) : 
					new I18nString(target.toString()));
			if (attributeMapper != null && groupMapper != null)
			{
				JsonNode jsonStatements = main.get("attributeStatements");
				int asLen = jsonStatements.size();
				List<AttributeStatement2> statements = new ArrayList<AttributeStatement2>(asLen);
				for (int i=0; i<asLen; i++)
				{
					try
					{
						statements.add(deserializeAS(jsonStatements.get(i), 
								groupMapper, attributeMapper));
					} catch (Exception e)
					{
						//OK - we are ignoring outdated ASes - will be removed by async cleanup
						outdatedASes++;
					}
				}			
				target.setAttributeStatements(statements.toArray(
						new AttributeStatement2[statements.size()]));
			}
			
			JsonNode jsonAcs = main.get("attributesClasses");
			int acLen = jsonAcs != null ? jsonAcs.size() : 0;
			Set<String> acs = new HashSet<>();
			for (int i=0; i<acLen; i++)
			{
				acs.add(jsonAcs.get(i).asText());
			}
			target.setAttributesClasses(acs);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
		return outdatedASes;
	}

	/**
	 * Only sets displayed name and description
	 * @param json
	 * @param target
	 */
	public void fillFromJsonMinimal(byte[] json, Group target)
	{
		if (json == null)
			return;
		try
		{
			ObjectNode main = mapper.readValue(json, ObjectNode.class);
			target.setDescription(I18nStringJsonUtil.fromJson(main.get("i18nDescription"),
					main.get("description")));
			target.setDisplayedName(main.has("displayedName") ? 
					I18nStringJsonUtil.fromJson(main.get("displayedName")) : 
					new I18nString(target.toString()));
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}
	

	/**
	 * Converts {@link GroupBean} into a {@link Group}. This is not in {@link GroupResolver} 
	 * as depends on {@link GroupsSerializer}
	 * @param gb
	 * @param mapper
	 * @return
	 */
	public Group resolveGroupBean(GroupBean gb, GroupsMapper mapper, AttributesMapper aMapper)
	{
		String path = groupResolver.resolveGroupPath(gb, mapper); 
		Group group = new Group(path);
		fromJson(gb.getContents(), group, mapper, aMapper);
		return group;
	}
	
	private JsonNode serializeAS(AttributeStatement2 as, GroupsMapper groupMapper, 
			AttributesMapper attributeMapper) 
			throws JsonProcessingException, IllegalGroupValueException, IllegalAttributeTypeException
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("resolution", as.getConflictResolution().name());

		main.put("condition", as.getCondition());
		if (as.getExtraAttributesGroup() != null)
		{
			GroupBean resolvedGroup = groupResolver.resolveGroup(as.getExtraAttributesGroup(), groupMapper);
			main.put("extraGroup", resolvedGroup.getId());
		}
		main.put("visibility", as.getDynamicAttributeVisibility().name());
		if (as.dynamicAttributeMode())
		{
			main.put("dynamicAttributeExpression", as.getDynamicAttributeExpression());
			main.put("dynamicAttributeName", as.getDynamicAttributeType().getName());
		} else
		{
			addAttributeToJson(main, "fixedAttribute-", as.getFixedAttribute(), 
					attributeMapper, groupMapper);
			
		}
		return main;
	}
	
	private void addAttributeToJson(ObjectNode main, String pfx, Attribute<?> attribute, AttributesMapper attributeMapper,
			GroupsMapper groupMapper) throws IllegalAttributeTypeException, IllegalGroupValueException
	{
		if (attribute == null)
			return;
		AttributeTypeBean atb = attributeResolver.resolveAttributeType(attribute.getName(), 
				attributeMapper);
		main.put(pfx+"attributeId", atb.getId());
		if (attribute.getGroupPath() != null)
		{
			GroupBean gb = groupResolver.resolveGroup(attribute.getGroupPath(), groupMapper);
			main.put(pfx+"attributeGroupId", gb.getId());
		}
		@SuppressWarnings({ "rawtypes", "unchecked" })
		byte[] attrValues = attributeSerializer.toJson(new AttributeExt(attribute, false, null, null));
		main.put(pfx+"attributeValues", attrValues);
	}
	
	private AttributeStatement2 deserializeAS(JsonNode as, GroupsMapper groupMapper, 
			AttributesMapper attributeMapper) throws IOException, IllegalGroupValueException, 
			IllegalTypeException, IllegalAttributeTypeException, WrongArgumentException, 
			IllegalAttributeValueException
	{
		AttributeStatement2 ret = new AttributeStatement2();
		String resolution = as.get("resolution").asText();
		ret.setConflictResolution(ConflictResolution.valueOf(resolution));

		ret.setCondition(as.get("condition").asText());
		
		if (as.has("extraGroup"))
		{
			long group = as.get("extraGroup").asLong();
			String groupPath = groupResolver.resolveGroupPath(group, groupMapper);
			ret.setExtraAttributesGroup(groupPath);
		}
		
		String visibility = as.get("visibility").asText();
		ret.setDynamicAttributeVisibility(AttributeVisibility.valueOf(visibility));
		
		Attribute<?> attr = getAttributeFromJson(as, "fixedAttribute-", attributeMapper, groupMapper);
		if (attr != null)
		{
			AttributeTypeBean atBean = attributeResolver.resolveAttributeType(attr.getName(), 
					attributeMapper);
			AttributeType at = attributeResolver.resolveAttributeTypeBean(atBean);
			AttributeValueChecker.validate(attr, at);
			ret.setFixedAttribute(attr);
		} else
		{
			ret.setDynamicAttributeExpression(as.get("dynamicAttributeExpression").asText());
			String aTypeName = as.get("dynamicAttributeName").asText();
			AttributeType aType = attributeResolver.resolveAttributeTypeFull(aTypeName, 
					attributeMapper);
			ret.setDynamicAttributeType(aType);
		}

		return ret;
	}
	
	private Attribute<?> getAttributeFromJson(JsonNode as, String pfx, AttributesMapper attributeMapper, 
			GroupsMapper groupMapper) throws IOException, IllegalTypeException, 
			IllegalAttributeTypeException, IllegalGroupValueException, IllegalAttributeValueException
	{
		if (!as.has(pfx+"attributeId"))
			return null;
		long attributeId = as.get(pfx+"attributeId").asLong();
		AttributeTypeBean atb = attributeMapper.getAttributeTypeById(attributeId);
		if (atb == null)
			throw new IllegalAttributeTypeException("The attribute type is not known " + attributeId);
		AttributeBean ab = new AttributeBean();
		ab.setName(atb.getName());
		ab.setValueSyntaxId(atb.getValueSyntaxId());
		ab.setValues(as.get(pfx+"attributeValues").binaryValue());
		String group = null;
		if (as.has(pfx+"attributeGroupId"))
		{
			long groupId = as.get(pfx+"attributeGroupId").asLong();
			group = groupResolver.resolveGroupPath(groupId, groupMapper);
		}
		Attribute<?> attribute = attributeResolver.resolveAttributeBean(ab, group);

		return attribute;
	}
}
