/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.ac.AttributeClassDB;
import pl.edu.icm.unity.db.json.AttributeSerializer;
import pl.edu.icm.unity.db.mapper.AttributesMapper;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.AttributeBean;
import pl.edu.icm.unity.db.model.AttributeTypeBean;
import pl.edu.icm.unity.db.model.DBLimits;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.model.GroupElementBean;
import pl.edu.icm.unity.db.resolvers.AttributesResolver;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.json.AttributeTypeSerializer;
import pl.edu.icm.unity.server.attributes.AttributeValueChecker;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Identity;


/**
 * Attributes related DB operations.
 * @author K. Benedyczak
 */
@Component
public class DBAttributes
{
	public static final String ATTRIBUTE_TYPES_NOTIFICATION_ID = "attributeTypes";
	private DBLimits limits;
	private AttributesResolver attrResolver;
	private AttributeTypeSerializer atSerializer;
	private AttributeSerializer aSerializer;
	private GroupResolver groupResolver;
	private DBShared dbShared;
	private AttributeStatementProcessor statementsHelper;
	private AttributeClassDB acDB;
	private DependencyNotificationManager notificationsManager;
	private DBIdentities dbIdentities;
	
	
	@Autowired
	public DBAttributes(DB db, AttributesResolver attrResolver,
			AttributeTypeSerializer atSerializer, AttributeSerializer aSerializer,
			GroupResolver groupResolver, DBShared dbShared,
			AttributeStatementProcessor statementsHelper, AttributeClassDB acDB,
			DependencyNotificationManager notificationsManager, DBIdentities dbIdentities)
	{
		this.dbIdentities = dbIdentities;
		this.limits = db.getDBLimits();
		this.attrResolver = attrResolver;
		this.atSerializer = atSerializer;
		this.aSerializer = aSerializer;
		this.groupResolver = groupResolver;
		this.dbShared = dbShared;
		this.statementsHelper = statementsHelper;
		this.acDB = acDB;
		this.notificationsManager = notificationsManager;
	}

	public void addAttributeType(AttributeType toAdd, SqlSession sqlMap) 
			throws EngineException
	{
		limits.checkNameLimit(toAdd.getName());
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		if (mapper.getAttributeType(toAdd.getName()) != null)
			throw new IllegalAttributeTypeException("The attribute type with name " + toAdd.getName() + 
					" already exists");
		
		AttributeTypeBean atb = new AttributeTypeBean(toAdd.getName(), atSerializer.toJson(toAdd), 
				toAdd.getValueType().getValueSyntaxId());
		notificationsManager.firePreAddEvent(ATTRIBUTE_TYPES_NOTIFICATION_ID, toAdd, sqlMap);
		mapper.insertAttributeType(atb);
	}

	
	public AttributeType getAttributeType(String id, SqlSession sqlMap) 
			throws IllegalAttributeTypeException, IllegalTypeException
	{
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		AttributeTypeBean atBean = attrResolver.resolveAttributeType(id, mapper);
		return attrResolver.resolveAttributeTypeBean(atBean);
	}
	
	public void removeAttributeType(String id, boolean withInstances, SqlSession sqlMap)
			throws EngineException
	{
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		if (mapper.getAttributeType(id) == null)
			throw new IllegalAttributeTypeException("The attribute type with name " + id + 
					" does not exist");
		if (!withInstances)
		{
			AttributeBean ab = new AttributeBean();
			ab.setName(id);
			if (mapper.getAttributes(ab).size() > 0)
				throw new IllegalAttributeTypeException("The attribute type " + id + " has instances");
		}
		AttributeTypeBean atBean = attrResolver.resolveAttributeType(id, mapper);
		AttributeType removed = attrResolver.resolveAttributeTypeBean(atBean);
		notificationsManager.firePreRemoveEvent(ATTRIBUTE_TYPES_NOTIFICATION_ID, removed, sqlMap);
		mapper.deleteAttributeType(id);
	}

	public void updateAttributeType(AttributeType toUpdate, SqlSession sqlMap) 
			throws EngineException
	{
		limits.checkNameLimit(toUpdate.getName());
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		GroupsMapper gMapper = sqlMap.getMapper(GroupsMapper.class);
		AttributeBean atb = new AttributeBean();
		atb.setName(toUpdate.getName());
		List<AttributeBean> allAttributesOfType = mapper.getAttributes(atb);
		for (AttributeBean ab: allAttributesOfType)
		{
			String groupPath = groupResolver.resolveGroupPath(ab.getGroupId(), gMapper);
			Attribute<?> attribute = attrResolver.resolveAttributeBean(ab, groupPath);
			try
			{
				AttributeValueChecker.validate(attribute, toUpdate);
			} catch (Exception e)
			{
				throw new IllegalAttributeTypeException("Can't update the attribute type as at least " +
						"one attribute instance will be in conflict with the new type. " +
						"The conflicting attribute which was found: " + attribute, e);
			}
		}
		AttributeTypeBean updatedB = new AttributeTypeBean(toUpdate.getName(), atSerializer.toJson(toUpdate), 
				toUpdate.getValueType().getValueSyntaxId());
		AttributeTypeBean atBean = attrResolver.resolveAttributeType(toUpdate.getName(), mapper);
		AttributeType old = attrResolver.resolveAttributeTypeBean(atBean);
		notificationsManager.firePreUpdateEvent(ATTRIBUTE_TYPES_NOTIFICATION_ID, old, toUpdate, sqlMap);
		mapper.updateAttributeType(updatedB);
	}
	
	public Map<String, AttributeType> getAttributeTypes(SqlSession sqlMap)
	{
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		List<AttributeTypeBean> raw = mapper.getAttributeTypes();
		Map<String, AttributeType> ret = new HashMap<String, AttributeType>(raw.size());
		for (int i=0; i<raw.size(); i++)
		{
			AttributeTypeBean r = raw.get(i);
			try
			{
				ret.put(r.getName(), attrResolver.resolveAttributeTypeBean(r));
			} catch (IllegalTypeException e)
			{
				throw new InternalException("Can not find implementation for attribtue type returned " +
						"by the getAttributeTypes() " + r.getName(), e);
			}
		}
		return ret;
	}
	
	
	private AttributeBean prepareAttributeParam(long entityId, long typeId, String attributeTypeName, String group, 
			AttributesMapper mapper, GroupsMapper groupsMapper) throws IllegalGroupValueException
	{
		GroupBean gr = groupResolver.resolveGroup(group, groupsMapper);
		AttributeBean param = new AttributeBean();
		param.setEntityId(entityId);
		param.setGroupId(gr.getId());
		param.setTypeId(typeId);
		param.setName(attributeTypeName);
		return param;
	}

	/**
	 * Adds an attribute to DB, creation and update timestamps are set to the current time.
	 * @param entityId
	 * @param attribute
	 * @param update
	 * @param sqlMap
	 * @throws IllegalAttributeValueException
	 * @throws IllegalTypeException
	 * @throws IllegalAttributeTypeException
	 * @throws IllegalGroupValueException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addAttribute(long entityId, Attribute<?> attribute, boolean update, SqlSession sqlMap) 
			throws IllegalAttributeValueException, IllegalTypeException, 
			IllegalAttributeTypeException, IllegalGroupValueException
	{
		Date now = new Date();
		addAttribute(entityId, new AttributeExt(attribute, true, now, now), update, sqlMap);
	}
	
	/**
	 * Adds an attribute to the local DB.
	 * @param entityId
	 * @param attribute
	 * @param update
	 * @param sqlMap
	 * @throws IllegalAttributeValueException
	 * @throws IllegalTypeException
	 * @throws IllegalAttributeTypeException
	 * @throws IllegalGroupValueException
	 */
	public void addAttribute(long entityId, AttributeExt<?> attribute, boolean update, SqlSession sqlMap) 
			throws IllegalAttributeValueException, IllegalTypeException, 
			IllegalAttributeTypeException, IllegalGroupValueException
	{
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		GroupsMapper grMapper = sqlMap.getMapper(GroupsMapper.class);

		AttributeTypeBean atBean = attrResolver.resolveAttributeType(attribute.getName(), mapper);
		AttributeType at = attrResolver.resolveAttributeTypeBean(atBean);
		AttributeValueChecker.validate(attribute, at);
		
		AttributeBean param = prepareAttributeParam(entityId, atBean.getId(), attribute.getName(),
				attribute.getGroupPath(), mapper, grMapper);
		List<AttributeBean> existing = mapper.getAttributes(param);
		if (existing.isEmpty())
		{
			if (grMapper.isMember(new GroupElementBean(param.getGroupId(), entityId)) == null)
				throw new IllegalGroupValueException("The entity is not a member "
						+ "of the group specified in the attribute");
			param.setValues(aSerializer.toJson(attribute));
			mapper.insertAttribute(param);
		} else
		{
			if (!update)
				throw new IllegalAttributeValueException("The attribute already exists");
			AttributeBean updated = existing.get(0);
			Long creationTs = aSerializer.getCreationTs(updated.getValues());
			attribute.setCreationTs(creationTs == null ? null : new Date(creationTs));
			param.setValues(aSerializer.toJson(attribute));
			mapper.updateAttribute(param);
		}
	}
	
	public void removeAttribute(long entityId, String groupPath, String attributeTypeName, SqlSession sqlMap) 
			throws IllegalAttributeValueException, IllegalAttributeTypeException, IllegalGroupValueException
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
	
	private List<String> getGroupsOrGroup(long entityId, String groupPath, GroupsMapper grMapper)
	{
		if (groupPath == null)
		{
			List<GroupBean> raw = grMapper.getGroups4Entity(entityId);
			List<String> ret = new ArrayList<String>();
			for (GroupBean gb: raw)
				ret.add(groupResolver.resolveGroupPath(gb, grMapper));
			return ret;
		} else
			return Collections.singletonList(groupPath);
	}
	
	/**
	 * 
	 * @param entityId
	 * @param groupPath
	 * @param sql
	 * @return set of attribute names of the given entity in a group
	 * @throws IllegalGroupValueException 
	 */
	public Collection<String> getEntityInGroupAttributeNames(long entityId, String groupPath, 
			SqlSession sql) throws IllegalGroupValueException
	{
		AttributesMapper atMapper = sql.getMapper(AttributesMapper.class);
		GroupsMapper gMapper = sql.getMapper(GroupsMapper.class);
		GroupBean grBean = groupResolver.resolveGroup(groupPath, gMapper);
		List<AttributeBean> allAts = getDefinedAttributes(entityId, grBean.getId(), null, atMapper);
		Set<String> ret = new HashSet<>();
		for (AttributeBean ab: allAts)
			ret.add(ab.getName());
		return ret;
	}
	
	/**
	 * Returns all attributes. Attribute name can be given or not.
	 * If the group is null, then attributes in all group scopes are returned.
	 * @param entityId
	 * @param groupPath
	 * @param attributeTypeName
	 * @param sql
	 * @return
	 * @throws EngineException 
	 * @throws WrongArgumentException 
	 */
	public Collection<AttributeExt<?>> getAllAttributes(long entityId, String groupPath, boolean effective, 
			String attributeTypeName, SqlSession sql) 
			throws EngineException
	{
		Map<String, Map<String, AttributeExt<?>>> asMap = getAllAttributesAsMap(entityId, groupPath, effective, 
				attributeTypeName, sql);
		List<AttributeExt<?>> ret = new ArrayList<AttributeExt<?>>();
		for (Map<String, AttributeExt<?>> entry: asMap.values())
			ret.addAll(entry.values());
		return ret;
	}
	
	public Map<String, AttributeExt<?>> getAllAttributesAsMapOneGroup(long entityId, String groupPath,
			String attributeTypeName, SqlSession sql) 
			throws EngineException
	{
		if (groupPath == null)
			throw new IllegalArgumentException("For this method group must be specified");
		Map<String, Map<String, AttributeExt<?>>> asMap = getAllAttributesAsMap(entityId, groupPath, true,  
				attributeTypeName, sql);
		return asMap.get(groupPath);
	}
	
	/**
	 * See {@link #getAllAttributes(long, String, String, SqlSession)}, the only difference is that the result
	 * is returned in a map indexed with groups (1st key) and attribute names (submap key).
	 * @param entityId
	 * @param groupPath
	 * @param attributeTypeName
	 * @param sql
	 * @return
	 * @throws EngineException 
	 * @throws WrongArgumentException 
	 */
	public Map<String, Map<String, AttributeExt<?>>> getAllAttributesAsMap(long entityId, String groupPath, 
			boolean effective, String attributeTypeName, SqlSession sql) 
			throws EngineException
	{
		AttributesMapper atMapper = sql.getMapper(AttributesMapper.class);
		GroupsMapper gMapper = sql.getMapper(GroupsMapper.class);
		
		Map<String, Map<String, AttributeExt<?>>> directAttributesByGroup = createAllAttrsMap(entityId, 
				atMapper, gMapper);
		if (!effective)
		{
			filterMap(directAttributesByGroup, groupPath, attributeTypeName);
			return directAttributesByGroup;
		}
		List<String> groups = getGroupsOrGroup(entityId, groupPath, gMapper);
		Set<String> allGroups = dbShared.getAllGroups(entityId, gMapper);
		Map<String, Map<String, AttributeExt<?>>> ret = new HashMap<String, Map<String, AttributeExt<?>>>();
		
		Map<String, AttributesClass> allClasses = acDB.getAllAsMap(sql);
		
		Identity[] identities = dbIdentities.getIdentitiesForEntity(entityId, null, false, sql);
		for (String group: groups)
		{
			Map<String, AttributeExt<?>> inGroup = statementsHelper.getEffectiveAttributes(identities, 
					group, attributeTypeName, allGroups, directAttributesByGroup, atMapper, 
					gMapper, allClasses);
			ret.put(group, inGroup);
		}
		return ret;
	}
	
	private void filterMap(Map<String, Map<String, AttributeExt<?>>> directAttributesByGroup,
			String groupPath, String attributeTypeName)
	{
		if (groupPath != null)
		{
			Map<String, AttributeExt<?>> v = directAttributesByGroup.get(groupPath); 
			directAttributesByGroup.clear();
			if (v != null)
				directAttributesByGroup.put(groupPath, v);
		}
		
		if (attributeTypeName != null)
		{
			for (Map<String, AttributeExt<?>> e: directAttributesByGroup.values())
			{
				AttributeExt<?> at = e.get(attributeTypeName);
				e.clear();
				if (at != null)
					e.put(attributeTypeName, at);
			}
		}
	}
	
	/**
	 * It is assumed that the attribute is single-value and mapped to string.
	 * Returned are all entities which have value of the attribute out of the given set.
	 * <p> 
	 * IMPORTANT! This is not taking into account effective attributes, and so it is usable only for certain system
	 * attributes.
	 * @param groupPath
	 * @param attributeTypeName
	 * @param value
	 * @param sql
	 * @return
	 * @throws IllegalTypeException 
	 * @throws IllegalGroupValueException 
	 */
	public Set<Long> getEntitiesBySimpleAttribute(String groupPath, String attributeTypeName, 
			Set<String> values, SqlSession sql) 
			throws IllegalTypeException, IllegalGroupValueException
	{
		GroupsMapper grMapper = sql.getMapper(GroupsMapper.class);
		AttributesMapper atMapper = sql.getMapper(AttributesMapper.class);

		GroupBean grBean = groupResolver.resolveGroup(groupPath, grMapper);
		List<AttributeBean> allAts = getDefinedAttributes(null, grBean.getId(), attributeTypeName, atMapper);
		
		Set<Long> ret = new HashSet<Long>();
		for (AttributeBean ab: allAts)
		{
			Attribute<?> attr = attrResolver.resolveAttributeBean(ab, groupPath);
			if (values.contains((String)attr.getValues().get(0)))
				ret.add(ab.getEntityId());
		}
		return ret;
	}
	
	
	/**
	 * It is assumed that the attribute is mapped to string.
	 * Returned are all entities which have value of the attribute among values of the given attribute
	 * in any group.
	 * @param attributeTypeName
	 * @param values
	 * @param sql
	 * @return
	 * @throws IllegalTypeException
	 * @throws IllegalGroupValueException
	 */
	public Set<Long> getEntitiesWithStringAttribute(String attributeTypeName, String value, SqlSession sql) 
			throws IllegalTypeException, IllegalGroupValueException
	{
		AttributesMapper atMapper = sql.getMapper(AttributesMapper.class);
		List<AttributeBean> allAts = getDefinedAttributes(null, null, attributeTypeName, atMapper);
		
		Set<Long> ret = new HashSet<Long>();
		for (AttributeBean ab: allAts)
		{
			Attribute<?> attr = attrResolver.resolveAttributeBean(ab, "/");
			for (Object av: attr.getValues())
				if (value.equals(av))
					ret.add(ab.getEntityId());
		}
		return ret;
	}
	
	/**
	 * @param entityId
	 * @param atMapper
	 * @param gMapper
	 * @return map indexed with groups. Values are maps of all attributes in a group, indexed with their names.
	 * @throws IllegalTypeException
	 * @throws IllegalGroupValueException
	 */
	private Map<String, Map<String, AttributeExt<?>>> createAllAttrsMap(long entityId, AttributesMapper atMapper,
			GroupsMapper gMapper) throws IllegalTypeException, IllegalGroupValueException
	{
		Map<String, Map<String, AttributeExt<?>>> ret = new HashMap<String, Map<String, AttributeExt<?>>>();
		List<AttributeBean> allAts = getDefinedAttributes(entityId, null, null, atMapper);
		for (AttributeBean ab: allAts)
		{
			String groupPath = groupResolver.resolveGroupPath(ab.getGroupId(), gMapper);
			AttributeExt<?> attribute = attrResolver.resolveAttributeBean(ab, groupPath);
			
			Map<String, AttributeExt<?>> attrsInGroup = ret.get(groupPath);
			if (attrsInGroup == null)
			{
				attrsInGroup = new HashMap<String, AttributeExt<?>>();
				ret.put(groupPath, attrsInGroup);
			}
			attrsInGroup.put(attribute.getName(), attribute);
		}
		return ret;
	}
	
	public List<AttributeBean> getDefinedAttributes(Long entityId, Long groupId, String attributeName, 
			AttributesMapper mapper)
	{
		AttributeBean param = new AttributeBean();
		param.setGroupId(groupId);
		param.setEntityId(entityId);
		param.setName(attributeName);
		return mapper.getAttributes(param);
	}
}













