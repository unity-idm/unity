/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;
import pl.edu.icm.unity.store.impl.attribute.AttributeJsonSerializer;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement2;
import pl.edu.icm.unity.types.basic.AttributeStatement2.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Group;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

	@Autowired
	private AttributeJsonSerializer attributeSerializer;
	
	@Autowired
	private AttributeTypeDAO atDAO;
	
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
	 * @return minial contents for the initialization of the root group '/'.
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
		for (AttributeStatement2 as: src.getAttributeStatements())
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
		AttributeStatement2[] statements = new AttributeStatement2[asLen];
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
	
	private JsonNode serializeAS(AttributeStatement2 as)
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
			main.put("dynamicAttributeName", as.getDynamicAttributeType().getName());
		} else if (as.getFixedAttribute() != null)
		{
			ObjectNode attrJson = attributeSerializer.toJson(as.getFixedAttribute());
			main.set("fixedAttribute", attrJson);
		}
		return main;
	}
	
	private AttributeStatement2 deserializeAS(JsonNode as)
	{
		AttributeStatement2 ret = new AttributeStatement2();
		String resolution = as.get("resolution").asText();
		ret.setConflictResolution(ConflictResolution.valueOf(resolution));

		ret.setCondition(as.get("condition").asText());
		
		if (as.has("extraGroupName"))
			ret.setExtraAttributesGroup(as.get("extraGroupName").asText());
		
		String visibility = as.get("visibility").asText();
		ret.setDynamicAttributeVisibility(AttributeVisibility.valueOf(visibility));
		
		if (as.has("fixedAttribute"))
		{
			Attribute<?> fixed = attributeSerializer.fromJson((ObjectNode) as.get("fixedAttribute"));
			ret.setFixedAttribute(fixed);
		} else if (as.has("dynamicAttributeName"))
		{
			ret.setDynamicAttributeExpression(as.get("dynamicAttributeExpression").asText());
			String aTypeName = as.get("dynamicAttributeName").asText();
			AttributeType aType = atDAO.get(aTypeName); 
			ret.setDynamicAttributeType(aType);
		}

		return ret;
	}

	@Override
	public Class<? extends Group> getClazz()
	{
		return Group.class;
	}
}
