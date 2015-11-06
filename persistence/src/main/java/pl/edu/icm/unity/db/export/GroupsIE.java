/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.export;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.mapper.AttributesMapper;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.BaseBean;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.types.basic.Group;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles import/export of groups table.
 * @author K. Benedyczak
 */
@Component
public class GroupsIE extends AbstractIE
{
	private final GroupResolver groupResolver;

	@Autowired
	public GroupsIE(ObjectMapper jsonMapper, GroupResolver groupResolver)
	{
		super(jsonMapper);
		this.groupResolver = groupResolver;
	}

	public void serialize(SqlSession sql, JsonGenerator jg) throws JsonGenerationException, 
			IOException, IllegalTypeException
	{
		Map<String, GroupBean> sortedGroups = getSortedGroups(sql, groupResolver); 
		
		jg.writeStartArray();
		for (Map.Entry<String, GroupBean> entry: sortedGroups.entrySet())
		{
			jg.writeStartObject();
			serializeBaseBeanToJson(jg, entry.getValue());
			jg.writeStringField("groupPath", entry.getKey());
			jg.writeEndObject();
		}
		jg.writeEndArray();
	}
	
	public void deserialize(SqlSession sql, JsonParser input, DumpHeader header) throws IOException, EngineException
	{
		GroupsMapper mapper = sql.getMapper(GroupsMapper.class);
		JsonUtils.expect(input, JsonToken.START_ARRAY);
		while(input.nextToken() == JsonToken.START_OBJECT)
		{
			GroupBean bean = new GroupBean();
			deserializeBaseBeanFromJson(input, bean);
			JsonUtils.nextExpect(input, "groupPath");
			String path = input.getValueAsString();
			JsonUtils.nextExpect(input, JsonToken.END_OBJECT);
			
			if (path.equals("/"))
				bean.setName(GroupResolver.ROOT_GROUP_NAME);
			else
			{
				String parent = new Group(path).getParentPath();
				GroupBean parentBean = groupResolver.resolveGroup(parent, mapper);
				bean.setParent(parentBean.getId());
			}
			mapper.insertGroup(bean);
		}
		JsonUtils.expect(input, JsonToken.END_ARRAY);
		
		if (header.getVersionMajor() == 1 && header.getVersionMinor() < 5)
			updateGroupStatements(sql);
	}
	
	/**
	 * Overriden to fix empty description
	 */
	@Override
	protected void deserializeBaseBeanFromJson(JsonParser input, BaseBean at) throws IOException
	{
		JsonUtils.nextExpect(input, "id");
		at.setId(input.getLongValue());
		JsonUtils.nextExpect(input, "name");
		at.setName(input.getText());
		JsonUtils.nextExpect(input, "contents");
		if (input.getCurrentToken() == JsonToken.VALUE_NULL)
		{
			at.setContents(null);
		} else
		{
			ObjectNode parsed = jsonMapper.readTree(input);
			JsonNode desc = parsed.get("description");
			if (desc == null || desc.isNull())
				parsed.put("description", "");
			
			at.setContents(jsonMapper.writeValueAsBytes(parsed));
		}		
	}
	
	public void updateGroupStatements(SqlSession sql) throws IOException, EngineException
	{
		GroupsMapper mapper = sql.getMapper(GroupsMapper.class);
		List<GroupBean> allGroups = mapper.getAllGroups();
		for (GroupBean group: allGroups)
		{
			convertStatements(sql, group);
			mapper.updateGroup(group);
		}
	}
	
	public static Map<String, GroupBean> getSortedGroups(SqlSession sql, GroupResolver groupResolver)
	{
		GroupsMapper mapper = sql.getMapper(GroupsMapper.class);
		List<GroupBean> beans = mapper.getAllGroups();
		Map<String, GroupBean> sortedGroups = new TreeMap<>(new Comparator<String>()
		{
			@Override
			public int compare(String o1, String o2)
			{
				if (o1.length() >= o2.length())
					return 1;
				else if (o1.length() < o2.length())
					return -1;
				else
					return o1.compareTo(o2);
			}

		});
		for (GroupBean bean: beans)
		{
			String path = groupResolver.resolveGroupPath(bean, mapper);
			sortedGroups.put(path, bean);
		}
		return sortedGroups;
	}
	
	private void convertStatements(SqlSession sql, GroupBean legacy) throws IOException, IllegalGroupValueException
	{
		ObjectNode root = (ObjectNode) jsonMapper.readTree(legacy.getContents());
		ArrayNode oldStatements = (ArrayNode) root.get("attributeStatements");
		if (oldStatements == null)
			return;
		
		ArrayNode newStatements = jsonMapper.createArrayNode();
		for (JsonNode statement: oldStatements)
			newStatements.add(convertStatement(statement, sql));
		root.set("attributeStatements", newStatements);
		legacy.setContents(jsonMapper.writeValueAsBytes(root));
	}
	
	private JsonNode convertStatement(JsonNode legacy, SqlSession sql) throws IllegalGroupValueException
	{
		AttributesMapper atMapper = sql.getMapper(AttributesMapper.class);
		GroupsMapper gMapper = sql.getMapper(GroupsMapper.class);
		ObjectNode updated = jsonMapper.createObjectNode();
		updated.set("resolution", legacy.get("resolution"));
		updated.put("visibility", "full");
		
		String type = legacy.get("type").asText();
		
		switch (type)
		{
		case "everybody":
			updated.put("condition", "true");
			copyLegacyToNew(legacy, updated);
			break;
		case "copyParentGroupAttribute":
		case "copySubgroupAttribute":
			String atName = atMapper.getAttributeTypeById(
					legacy.get("condition-attributeId").asLong()).getName();
			updated.put("condition", "eattrs contains '" + atName + "'");
			updated.put("extraGroup", legacy.get("condition-attributeGroupId").asText());
			updated.put("dynamicAttributeName", atName);
			updated.put("dynamicAttributeExpression", "eattrs['" + atName + "']");
			break;
		case "hasParentgroupAttribute":
		case "hasSubgroupAttribute":
			atName = atMapper.getAttributeTypeById(
					legacy.get("condition-attributeId").asLong()).getName();
			updated.put("condition", "eattrs contains '" + atName + "'");
			updated.put("extraGroup", legacy.get("condition-attributeGroupId").asText());
			copyLegacyToNew(legacy, updated);
			break;
		case "memberOf":
			String group = groupResolver.resolveGroupPath(legacy.get("conditionGroup").asLong(), gMapper);
			updated.put("condition", "groups contains '" + group + "'");
			copyLegacyToNew(legacy, updated);
			break;
		}
		
		return updated;
	}
	
	private void copyLegacyToNew(JsonNode legacy, ObjectNode updated)
	{
		updated.put("fixedAttribute-attributeId", legacy.get("assigned-attributeId").asText());
		updated.put("fixedAttribute-attributeGroupId", legacy.get("assigned-attributeGroupId").asText());
		updated.put("fixedAttribute-attributeValues", legacy.get("assigned-attributeValues").asText());
	}
}

