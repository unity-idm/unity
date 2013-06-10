/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
import pl.edu.icm.unity.server.registries.AttributeStatementsRegistry;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;

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
			main.put("description", src.getDescription());
			ArrayNode ases = main.putArray("attributeStatements");
			for (AttributeStatement as: src.getAttributeStatements())
			{
				ases.add(serializeAS(as, groupMapper, attributeMapper));
			}
			
			return mapper.writeValueAsBytes(main);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}
	
	/**
	 * Fills target with JSON contents, checking it for correctness
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
			target.setDescription(main.get("description").asText());
			JsonNode jsonStatements = main.get("attributeStatements");
			int asLen = jsonStatements.size();
			List<AttributeStatement> statements = new ArrayList<AttributeStatement>(asLen);
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
			target.setAttributeStatements(statements.toArray(new AttributeStatement[statements.size()]));
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
		return outdatedASes;
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
	
	private JsonNode serializeAS(AttributeStatement as, GroupsMapper groupMapper, 
			AttributesMapper attributeMapper) 
			throws JsonProcessingException, IllegalGroupValueException, IllegalAttributeTypeException
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("resolution", as.getConflictResolution().name());

		addAttributeToJson(main, "assigned-", as.getAssignedAttribute(), attributeMapper, groupMapper);
		addAttributeToJson(main, "condition-", as.getConditionAttribute(), attributeMapper, groupMapper);
		if (as.getConditionGroup() != null)
		{
			GroupBean gb = groupResolver.resolveGroup(as.getConditionGroup(), groupMapper);
			main.put("conditionGroup", gb.getId());
		}
		main.put("type", as.getName());
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
		byte[] attrValues = attributeSerializer.toJson(attribute);
		main.put(pfx+"attributeValues", attrValues);
	}
	
	private AttributeStatement deserializeAS(JsonNode as, GroupsMapper groupMapper, 
			AttributesMapper attributeMapper) throws IOException, IllegalGroupValueException, 
			IllegalTypeException, IllegalAttributeTypeException, WrongArgumentException, 
			IllegalAttributeValueException
	{
		String type = as.get("type").asText();
		AttributeStatement ret = AttributeStatementsRegistry.getInstance(type);
		String resolution = as.get("resolution").asText();
		ret.setConflictResolution(ConflictResolution.valueOf(resolution));
		
		Attribute<?> attr = getAttributeFromJson(as, "assigned-", attributeMapper, groupMapper);
		if (attr != null)
		{
			AttributeTypeBean atBean = attributeResolver.resolveAttributeType(attr.getName(), 
					attributeMapper);
			AttributeType at = attributeResolver.resolveAttributeTypeBean(atBean);
			AttributeValueChecker.validate(attr, at);
			ret.setAssignedAttribute(attr);
		}
		
		Attribute<?> condAttr = getAttributeFromJson(as, "condition-", attributeMapper, groupMapper);
		ret.setConditionAttribute(condAttr);

		if (as.has("conditionGroup"))
		{
			long group = as.get("conditionGroup").asLong();
			String groupPath = groupResolver.resolveGroupPath(group, groupMapper);
			ret.setConditionGroup(groupPath);
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
