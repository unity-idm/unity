/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.JsonSerializer;
import pl.edu.icm.unity.db.json.SerializersRegistry;
import pl.edu.icm.unity.db.mapper.AttributesMapper;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.AttributeBean;
import pl.edu.icm.unity.db.model.AttributeTypeBean;
import pl.edu.icm.unity.db.model.DBLimits;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.model.GroupElementBean;
import pl.edu.icm.unity.db.resolvers.AttributesResolver;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;


/**
 * Attributes related DB operations.
 * @author K. Benedyczak
 */
@Component
public class DBAttributes
{
	private DBLimits limits;
	private AttributesResolver attrResolver;
	private JsonSerializer<AttributeType> atSerializer;
	@SuppressWarnings("rawtypes")
	private JsonSerializer<Attribute> aSerializer;
	private GroupResolver groupResolver;
	
	@Autowired
	public DBAttributes(DB db, SerializersRegistry registry, AttributesResolver attrResolver, 
			GroupResolver groupResolver)
	{
		this.limits = db.getDBLimits();
		this.atSerializer = registry.getSerializer(AttributeType.class);
		this.aSerializer = registry.getSerializer(Attribute.class);
		this.groupResolver = groupResolver;
		this.attrResolver = attrResolver;
	}
	
	public void addAttributeType(AttributeType toAdd, SqlSession sqlMap)
	{
		if (toAdd.getName().length() > limits.getNameLimit())
			throw new IllegalGroupValueException("Attribute type name length must not exceed " + 
					limits.getNameLimit() + " characters");
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		if (mapper.getAttributeType(toAdd.getName()) != null)
			throw new IllegalAttributeTypeException("The attribute type with name " + toAdd.getName() + 
					" already exists");
		
		AttributeTypeBean atb = new AttributeTypeBean(toAdd.getName(), atSerializer.toJson(toAdd), 
				toAdd.getValueType().getValueSyntaxId());
		mapper.insertAttributeType(atb);
	}
	
	public List<AttributeType> getAttributeTypes(SqlSession sqlMap)
	{
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		List<AttributeTypeBean> raw = mapper.getAttributeTypes();
		List<AttributeType> ret = new ArrayList<AttributeType>(raw.size());
		for (int i=0; i<raw.size(); i++)
		{
			AttributeTypeBean r = raw.get(i);
			ret.add(attrResolver.resolveAttributeTypeBean(r));
		}
		return ret;
	}
	
	
	private AttributeBean prepareAttributeParam(long entityId, long typeId, String attributeTypeName, String group, 
			AttributesMapper mapper, GroupsMapper groupsMapper)
	{
		GroupBean gr = groupResolver.resolveGroup(group, groupsMapper);
		AttributeBean param = new AttributeBean();
		param.setEntityId(entityId);
		param.setGroupId(gr.getId());
		param.setTypeId(typeId);
		param.setName(attributeTypeName);
		return param;
	}
	
	public void addAttribute(long entityId, Attribute<?> attribute, boolean update, SqlSession sqlMap)
	{
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		GroupsMapper grMapper = sqlMap.getMapper(GroupsMapper.class);

		AttributeTypeBean atBean = attrResolver.resolveAttributeType(attribute.getName(), mapper);
		AttributeType at = attrResolver.resolveAttributeTypeBean(atBean);
		AttributeValueChecker.validate(attribute, at);
		
		AttributeBean param = prepareAttributeParam(entityId, atBean.getId(), attribute.getName(),
				attribute.getGroupPath(), mapper, grMapper);
		AttributeBean existing = mapper.getSpecificAttribute(param);
		if (existing == null)
		{
			if (grMapper.isMember(new GroupElementBean(param.getGroupId(), entityId)) == null)
				throw new IllegalGroupValueException("The entity is not a member of the group specified in the attribute");
			param.setValues(aSerializer.toJson(attribute));
			mapper.insertAttribute(param);
		} else
		{
			if (!update)
				throw new IllegalAttributeValueException("The attribute already exists");
			param.setValues(aSerializer.toJson(attribute));
			mapper.updateAttribute(param);
		}
	}
	
	public void removeAttribute(long entityId, String groupPath, String attributeTypeName, SqlSession sqlMap)
	{
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		GroupsMapper grMapper = sqlMap.getMapper(GroupsMapper.class);
		
		AttributeTypeBean atBean = attrResolver.resolveAttributeType(attributeTypeName, mapper);
		AttributeBean param = prepareAttributeParam(entityId, atBean.getId(), attributeTypeName,
				groupPath, mapper, grMapper);
		AttributeBean existing = mapper.getSpecificAttribute(param);
		if (existing == null)
			throw new IllegalAttributeValueException("The attribute does not exist");
		
		mapper.deleteAttribute(param);
	}
	
	
	private List<AttributeBean> getDefinedAttributes(long entityId, long groupId, String attributeName, 
			AttributesMapper mapper)
	{
		AttributeBean param = new AttributeBean();
		param.setEntityId(entityId);
		param.setGroupId(groupId);
		param.setEntityId(entityId);
		
		if (attributeName != null)
		{
			param.setName(attributeName);
			return Collections.singletonList(mapper.getSpecificAttribute(param));
		} else
		{
			return mapper.getInGroupAttributes(param);
		}
	}
	
	/**
	 * Returns all attributes of a given entity in a specified group. Optionally the attribute name can be fixed.
	 * @param entityId
	 * @param groupPath
	 * @param attributeTypeName
	 * @param sql
	 * @return
	 */
	public List<Attribute<?>> getAllAttributes(long entityId, String groupPath,
			String attributeTypeName, SqlSession sql)
	{
		List<GroupBean> groups;
		GroupsMapper grMapper = sql.getMapper(GroupsMapper.class);
		if (groupPath == null)
			groups = grMapper.getGroups4Entity(entityId);
		else
			groups = Collections.singletonList(groupResolver.resolveGroup(groupPath, grMapper));
		
		AttributesMapper atMapper = sql.getMapper(AttributesMapper.class);
		
		List<Attribute<?>> ret = new ArrayList<Attribute<?>>();
		for (GroupBean group: groups)
		{
			List<AttributeBean> raw = getDefinedAttributes(entityId, group.getId(), 
					attributeTypeName, atMapper);
			
			//TODO here we will need to insert application of group rule-defined adding additional attributes  
			
			ret.addAll(attrResolver.convertAttributes(raw, groupPath));
		}
		return ret;
	}
}













