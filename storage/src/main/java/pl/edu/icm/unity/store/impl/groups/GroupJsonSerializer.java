/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Group;

/**
 * Serialization to from Json and to from RDBMS beans. 
 * @author K. Benedyczak
 */
@Component
public class GroupJsonSerializer implements RDBMSObjectSerializer<Group, GroupBean>, 
			JsonSerializerForKryo<Group>
{
	@Autowired
	private ObjectMapper mapper;

	@Override
	public Group fromJson(ObjectNode main)
	{
		Group ret = new Group(main.get("path").asText());
		fromJsonBase(main, ret);
		return ret;
	}

	@Override
	public ObjectNode toJson(Group src)
	{
		ObjectNode main = toJsonBase(src);
		main.put("path", src.toString());
		return main;
	}

	@Override
	public GroupBean toDB(Group object)
	{
		GroupBean gb = new GroupBean(object.toString(), object.getParentPath());
		gb.setContents(JsonUtil.serialize2Bytes(toJsonBase(object)));
		return gb;
	}

	@Override
	public Group fromDB(GroupBean bean)
	{
		Group ret = new Group(bean.getName());
		fromJsonBase(JsonUtil.parse(bean.getContents()), ret);
		return ret;
	}
	
	/**
	 * @return minimal contents for the initialization of the root group '/'.
	 * Needs to be static as it is created early on startup when real DAO infrastructure is not ready.
	 */
	public static ObjectNode createRootGroupContents()
	{
		ObjectNode main = new ObjectMapper().createObjectNode();
		main.set("i18nDescription", I18nStringJsonUtil.toJson(null));
		main.set("displayedName", I18nStringJsonUtil.toJson(null));
		main.putArray("attributeStatements");
		main.putArray("attributesClasses");
		return main;
	}
	
	private ObjectNode toJsonBase(Group src)
	{
		ObjectNode main = mapper.createObjectNode();
		main.set("i18nDescription", I18nStringJsonUtil.toJson(src.getDescription()));
		main.set("displayedName", I18nStringJsonUtil.toJson(src.getDisplayedName()));
		ArrayNode ases = main.putArray("attributeStatements");
		for (AttributeStatement as: src.getAttributeStatements())
			ases.add(serializeAS(as));
		ArrayNode aces = main.putArray("attributesClasses");
		for (String ac: src.getAttributesClasses())
			aces.add(ac);
		return main;
	}

	private void fromJsonBase(ObjectNode main, Group target)
	{
		target.setDescription(I18nStringJsonUtil.fromJson(main.get("i18nDescription"),
				main.get("description")));
		target.setDisplayedName(main.has("displayedName") ? 
				I18nStringJsonUtil.fromJson(main.get("displayedName")) : 
					new I18nString(target.toString()));
		
		ArrayNode jsonStatements = (ArrayNode) main.get("attributeStatements");
		int asLen = jsonStatements.size();
		AttributeStatement[] statements = new AttributeStatement[asLen];
		int i=0;
		for (JsonNode n: jsonStatements)
			statements[i++] = deserializeAS(n);
		target.setAttributeStatements(statements);

		ArrayNode jsonAcs = (ArrayNode) main.get("attributesClasses");
		Set<String> acs = new HashSet<>();
		for (JsonNode e: jsonAcs)
			acs.add(e.asText());
		target.setAttributesClasses(acs);
	}
	
	private JsonNode serializeAS(AttributeStatement as)
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("resolution", as.getConflictResolution().name());

		main.put("condition", as.getCondition());
		if (as.getExtraAttributesGroup() != null)
			main.put("extraGroupName", as.getExtraAttributesGroup());
		main.put("visibility", as.getDynamicAttributeVisibility().name());
		if (as.dynamicAttributeMode())
		{
			main.put("dynamicAttributeExpression", as.getDynamicAttributeExpression());
			main.put("dynamicAttributeName", as.getDynamicAttributeType());
		} else if (as.getFixedAttribute() != null)
		{
			ObjectNode attrJson = as.getFixedAttribute().toJson();
			main.set("fixedAttribute", attrJson);
		}
		return main;
	}
	
	private AttributeStatement deserializeAS(JsonNode as)
	{
		AttributeStatement ret = new AttributeStatement();
		String resolution = as.get("resolution").asText();
		ret.setConflictResolution(ConflictResolution.valueOf(resolution));

		ret.setCondition(as.get("condition").asText());
		
		if (as.has("extraGroupName"))
			ret.setExtraAttributesGroup(as.get("extraGroupName").asText());
		
		String visibility = as.get("visibility").asText();
		ret.setDynamicAttributeVisibility(AttributeVisibility.valueOf(visibility));
		
		if (as.has("fixedAttribute"))
		{
			Attribute fixed = new Attribute((ObjectNode) as.get("fixedAttribute"));
			ret.setFixedAttribute(fixed);
		} else if (as.has("dynamicAttributeName"))
		{
			ret.setDynamicAttributeExpression(as.get("dynamicAttributeExpression").asText());
			String aTypeName = as.get("dynamicAttributeName").asText();
			ret.setDynamicAttributeType(aTypeName);
		}

		return ret;
	}

	@Override
	public Class<? extends Group> getClazz()
	{
		return Group.class;
	}
}
