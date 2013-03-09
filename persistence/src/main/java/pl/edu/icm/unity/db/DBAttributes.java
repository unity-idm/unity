/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		limits.checkNameLimit(toAdd.getName());
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
		List<AttributeBean> existing = mapper.getAttributes(param);
		if (existing.size() == 0)
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
		List<AttributeBean> existing = mapper.getAttributes(param);
		if (existing.size() == 0)
			throw new IllegalAttributeValueException("The attribute does not exist");
		
		mapper.deleteAttribute(param);
	}
	
	
	private List<AttributeBean> getDefinedAttributes(Long entityId, Long groupId, String attributeName, 
			AttributesMapper mapper)
	{
		AttributeBean param = new AttributeBean();
		param.setGroupId(groupId);
		param.setEntityId(entityId);
		param.setName(attributeName);
		return mapper.getAttributes(param);
	}
	
	private List<GroupBean> getGroupsOrGroup(long entityId, String groupPath, SqlSession sql)
	{
		GroupsMapper grMapper = sql.getMapper(GroupsMapper.class);
		if (groupPath == null)
			return grMapper.getGroups4Entity(entityId);
		else
			return Collections.singletonList(groupResolver.resolveGroup(groupPath, grMapper));
	}
	
	/**
	 * Returns all attributes. Attribute name can be given or not.
	 * If the group is null, then attributes in all group scopes are returned.
	 * @param entityId
	 * @param groupPath
	 * @param attributeTypeName
	 * @param sql
	 * @return
	 */
	public List<Attribute<?>> getAllAttributes(long entityId, String groupPath,
			String attributeTypeName, SqlSession sql)
	{
		List<GroupBean> groups = getGroupsOrGroup(entityId, groupPath, sql);
		
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

	/**
	 * See {@link #getAllAttributes(long, String, String, SqlSession)}, the only difference is that the result
	 * is returned in a map indexed with attribute names
	 * @param entityId
	 * @param groupPath
	 * @param attributeTypeName
	 * @param sql
	 * @return
	 */
	public Map<String, Attribute<?>> getAllAttributesAsMap(long entityId, String groupPath,
			String attributeTypeName, SqlSession sql)
	{
		List<GroupBean> groups = getGroupsOrGroup(entityId, groupPath, sql);
		AttributesMapper atMapper = sql.getMapper(AttributesMapper.class);
		
		Map<String, Attribute<?>> ret = new HashMap<String, Attribute<?>>();
		for (GroupBean group: groups)
		{
			List<AttributeBean> raw = getDefinedAttributes(entityId, group.getId(), 
					attributeTypeName, atMapper);
			
			//TODO here we will need to insert application of group rule-defined adding additional attributes
			//of course reuse the same code as in the method above
			
			List<Attribute<?>> attributes = attrResolver.convertAttributes(raw, groupPath);
			for (Attribute<?> a: attributes)
				ret.put(a.getName(), a);
		}
		return ret;
	}
	
	/**
	 * It is assumed that the attribute is single-value and mapped to string 
	 * @param groupPath
	 * @param attributeTypeName
	 * @param value
	 * @param sql
	 * @return
	 */
	public Set<Long> getEntitiesBySimpleAttribute(String groupPath, String attributeTypeName, 
			String value, SqlSession sql)
	{
		GroupsMapper grMapper = sql.getMapper(GroupsMapper.class);
		AttributesMapper atMapper = sql.getMapper(AttributesMapper.class);

		GroupBean grBean = groupResolver.resolveGroup(groupPath, grMapper);
		List<AttributeBean> allAts = getDefinedAttributes(null, grBean.getId(), attributeTypeName, atMapper);
		
		Set<Long> ret = new HashSet<Long>();
		for (AttributeBean ab: allAts)
		{
			Attribute<?> attr = attrResolver.resolveAttributeBean(ab, groupPath);
			if (value.equals((String)attr.getValues().get(0)))
				ret.add(ab.getEntityId());
		}
		return ret;
	}
}













