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
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatementCondition.Type;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeStatementCondition;
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
	 */
	public byte[] toJson(Group src, GroupsMapper groupMapper, 
			AttributesMapper attributeMapper)
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
	public void fromJson(byte[] json, Group target, GroupsMapper groupMapper, 
			AttributesMapper attributeMapper)
	{
		if (json == null)
			return;
		ObjectNode main;
		try
		{
			main = mapper.readValue(json, ObjectNode.class);
			target.setDescription(main.get("description").asText());
			JsonNode jsonStatements = main.get("attributeStatements");
			int asLen = jsonStatements.size();
			List<AttributeStatement> statements = new ArrayList<AttributeStatement>(asLen);
			String path = target.toString();
			for (int i=0; i<asLen; i++)
			{
				try
				{
					statements.add(deserializeAS(jsonStatements.get(i), path, 
							groupMapper, attributeMapper));
				} catch (Exception e)
				{
					//OK - we are ignoring outdated ASes - will be removed by async cleanup
				}
			}			
			target.setAttributeStatements(statements.toArray(new AttributeStatement[statements.size()]));
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}

	private JsonNode serializeAS(AttributeStatement as, GroupsMapper groupMapper, 
			AttributesMapper attributeMapper) throws JsonProcessingException
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("resolution", as.getConflictResolution().name());

		addAttributeToJson(main, as.getAssignedAttribute(), attributeMapper, groupMapper);
		
		ArrayNode conditions = main.putArray("conditions");
		for (AttributeStatementCondition asc: as.getConditions())
		{
			conditions.add(serializeASCond(asc, groupMapper, attributeMapper));
		}
		return main;
	}
	
	private JsonNode serializeASCond(AttributeStatementCondition asc, GroupsMapper groupMapper, 
			AttributesMapper attributeMapper)
	{
		ObjectNode main = mapper.createObjectNode();

		if (asc.getGroup() != null)
		{
			GroupBean gb = groupResolver.resolveGroup(asc.getGroup(), groupMapper);
			main.put("group", gb.getId());
		}
	
		if (asc.getAttribute() != null)
		{
			addAttributeToJson(main, asc.getAttribute(), attributeMapper, groupMapper);
		}
		
		main.put("type", asc.getType().name());
		return main;
	}
	
	private void addAttributeToJson(ObjectNode main, Attribute<?> attribute, AttributesMapper attributeMapper,
			GroupsMapper groupMapper)
	{
		AttributeTypeBean atb = attributeResolver.resolveAttributeType(attribute.getName(), 
				attributeMapper);
		main.put("attributeId", atb.getId());
		if (attribute.getGroupPath() != null)
		{
			GroupBean gb = groupResolver.resolveGroup(attribute.getGroupPath(), groupMapper);
			main.put("attributeGroupId", gb.getId());
		}
		byte[] attrValues = attributeSerializer.toJson(attribute);
		main.put("attributeValues", attrValues);
	}
	
	private AttributeStatement deserializeAS(JsonNode as, String group, GroupsMapper groupMapper, 
			AttributesMapper attributeMapper) throws IOException
	{
		AttributeStatement ret = new AttributeStatement();
		String resolution = as.get("resolution").asText();
		ret.setConflictResolution(ConflictResolution.valueOf(resolution));
		
		Attribute<?> attr = getAttributeFromJson(as, attributeMapper, groupMapper);
		attr.setGroupPath(group);
		ret.setAssignedAttribute(attr);
		
		JsonNode jsonConditions = as.get("conditions");
		int conditionsNum = jsonConditions.size();
		AttributeStatementCondition[] conditions = new AttributeStatementCondition[conditionsNum];
		for (int i=0; i<conditionsNum; i++)
			conditions[i] = deserializeASCond(jsonConditions.get(i), groupMapper, attributeMapper);
		ret.setConditions(conditions);
		return ret;
	}
	
	private AttributeStatementCondition deserializeASCond(JsonNode asc, GroupsMapper groupMapper, 
			AttributesMapper attributeMapper) throws IOException
	{
		AttributeStatementCondition ret = new AttributeStatementCondition();
		String type = asc.get("type").asText();
		ret.setType(Type.valueOf(type));
		
		if (asc.has("group"))
		{
			long group = asc.get("group").asLong();
			String groupPath = groupResolver.resolveGroupPath(group, groupMapper);
			ret.setGroup(groupPath);
		}
		
		if (asc.has("attributeId"))
		{
			Attribute<?> attribute = getAttributeFromJson(asc, attributeMapper, groupMapper); 
			ret.setAttribute(attribute);
		}
		
		return ret;
	}

	private Attribute<?> getAttributeFromJson(JsonNode as, AttributesMapper attributeMapper, 
			GroupsMapper groupMapper) throws IOException
	{
		long attributeId = as.get("attributeId").asLong();
		AttributeTypeBean atb = attributeMapper.getAttributeTypeById(attributeId);
		if (atb == null)
			throw new IllegalAttributeTypeException("The attribute type is not known " + attributeId);
		AttributeBean ab = new AttributeBean();
		ab.setName(atb.getName());
		ab.setValueSyntaxId(atb.getValueSyntaxId());
		ab.setValues(as.get("attributeValues").binaryValue());
		String group = null;
		if (as.has("attributeGroupId"))
		{
			long groupId = as.get("attributeGroupId").asLong();
			group = groupResolver.resolveGroupPath(groupId, groupMapper);
		}
		return attributeResolver.resolveAttributeBean(ab, group);
	}
}
