/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.GroupsSerializer;
import pl.edu.icm.unity.db.mapper.AttributesMapper;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.AttributeTypeBean;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.resolvers.AttributesResolver;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatementCondition;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeStatementCondition.Type;
import pl.edu.icm.unity.types.basic.Group;

/**
 * Immutable class handling group attribute statements.
 * @author K. Benedyczak
 */
@Component
public class AttributeStatementProcessor
{
	private enum CollectionMode {all, downwards, upwards}
	private GroupResolver groupResolver;
	private AttributesResolver attrResolver;
	private GroupsSerializer jsonS;
	
	@Autowired
	public AttributeStatementProcessor(GroupResolver groupResolver,
			AttributesResolver attrResolver, GroupsSerializer jsonS)
	{
		this.groupResolver = groupResolver;
		this.attrResolver = attrResolver;
		this.jsonS = jsonS;
	}


	/**
	 * Collects all attributes for the given entity in the given group.
	 * The algorithm is as follows:
	 * <ol>
	 *  <li> effective attributes are collected in all subgroups, which are mentioned in attribute statements
	 *  conditions based on subgroup attributes. This process is recursive, but all statements related to 
	 *  parent groups are ignored.
	 *  <li> effective attributes are collected in the parent group, if it is mentioned in at least one 
	 *  attribute statement condition, based on parent group attributes. This process is recursive, 
	 *  but all statements related to subgroups groups are ignored.
	 *  <li> statements for this group are processed. For conditions evaluation data from the above steps 
	 *  and method arguments is used.
	 * </ol>
	 * @param entityId
	 * @param group
	 * @param attribute the only interesting attribute or null if all should be collected
	 * @param allGroups set with all groups where the entity is the member
	 * @param directAttributesByGroup map with group as keys with all regular attributes of the user. Values
	 * are maps of attributes by name.
	 * @param atMapper
	 * @param gMapper
	 * @return collected attributes in a map form. Map keys are attribute names.
	 */
	public Map<String, Attribute<?>> getEffectiveAttributes(long entityId, String group, String attribute, 
			Set<String> allGroups, Map<String, Map<String, Attribute<?>>> directAttributesByGroup,
			AttributesMapper atMapper, GroupsMapper gMapper)
	{
		Map<String, Map<String, Attribute<?>>> downwardsAttributes = new HashMap<String, Map<String,Attribute<?>>>();
		collectUpOrDownAttributes(CollectionMode.downwards, group, attribute, downwardsAttributes, 
				directAttributesByGroup, allGroups, atMapper, gMapper);

		Map<String, Map<String, Attribute<?>>> upwardsAttributes = new HashMap<String, Map<String,Attribute<?>>>();
		collectUpOrDownAttributes(CollectionMode.upwards, group, attribute, upwardsAttributes, 
				directAttributesByGroup, allGroups, atMapper, gMapper);

		AttributeStatement[] statements = getGroupStatements(group, atMapper, gMapper);
		
		return processAttributeStatements(CollectionMode.all, directAttributesByGroup, 
				upwardsAttributes, downwardsAttributes, group, 
				attribute, statements, allGroups, atMapper);
	}

	/**
	 * Resolves group path and returns group's attribute statements
	 * @param groupPath
	 * @param mapper
	 * @param gMapper
	 * @return 
	 */
	private AttributeStatement[] getGroupStatements(String groupPath, AttributesMapper mapper, 
			GroupsMapper gMapper)
	{
		GroupBean groupBean = groupResolver.resolveGroup(groupPath, gMapper);
		Group group = jsonS.resolveGroupBean(groupBean, gMapper, mapper);
		return group.getAttributeStatements(); 
	}
	
	/**
	 * Recursive method collection attributes in down or up direction. Works as follows:
	 * <ol>
	 * <li> statements of the group are established
	 * <li> for each statement which has condition related to an attribute in a other group in the 
	 * direction of the mode, the group is recorded to a set.
	 * <li> for each group from the set recursive call is made
	 * <li> normal processing of the statements of this group is performed, however only the input
	 * for rules related to the groups in the mode is provided. Statements in opposite direction are ignored. 
	 * </ol>
	 * @param mode
	 * @param groupPath
	 * @param upOrDownAttributes
	 * @param allAttributesByGroup
	 * @param allGroups
	 * @param mapper
	 * @param gMapper
	 */
	private void collectUpOrDownAttributes(CollectionMode mode, String groupPath, String attribute,
			Map<String, Map<String, Attribute<?>>> upOrDownAttributes, 
			Map<String, Map<String, Attribute<?>>> allAttributesByGroup,
			Set<String> allGroups, AttributesMapper mapper, GroupsMapper gMapper)
	{
		AttributeStatement[] statements = getGroupStatements(groupPath, mapper, gMapper);
		
		Set<String> interestingGroups = new HashSet<String>();
		for (AttributeStatement as: statements)
		{
			Type type = as.getCondition().getType();
			if (isUpOrDownStatement(mode, type) && isForInterestingAttribute(attribute, as))
			{
				String groupPath2 = as.getCondition().getAttribute().getGroupPath();
				interestingGroups.add(groupPath2);
			}
		}
		for (String interestingGroup: interestingGroups)
		{
			collectUpOrDownAttributes(mode, interestingGroup, attribute, upOrDownAttributes, allAttributesByGroup,
					allGroups, mapper, gMapper);
		}
		
		Map<String, Attribute<?>> ret = (mode == CollectionMode.downwards) ? 
				processAttributeStatements(mode, allAttributesByGroup, null, upOrDownAttributes, 
						groupPath, null, statements, allGroups, mapper) 
				:
				processAttributeStatements(mode, allAttributesByGroup, upOrDownAttributes, null, 
						groupPath, null, statements, allGroups, mapper) ;
		upOrDownAttributes.put(groupPath, ret);
	}
	
	private boolean isForInterestingAttribute(String attribute, AttributeStatement as)
	{
		if (attribute == null)
			return true;
		if (as.getAssignedAttribute().getName().equals(attribute))
			return true;
		return false;
	}
	
	private boolean isUpOrDownStatement(CollectionMode mode, Type type)
	{
		if (mode == CollectionMode.downwards && 
				(type == Type.hasSubgroupAttribute || type == Type.hasSubgroupAttributeValue))
			return true;
		if (mode == CollectionMode.upwards && 
				(type == Type.hasParentgroupAttribute || type == Type.hasParentgroupAttributeValue))
			return true;
		return false;
	}
	
	private Map<String, Attribute<?>> processAttributeStatements(
			CollectionMode mode,
			Map<String, Map<String, Attribute<?>>> allAttributesByGroup,
			Map<String, Map<String, Attribute<?>>> upwardsAttributesByGroup,
			Map<String, Map<String, Attribute<?>>> downwardsAttributesByGroup,
			String group, String attribute, AttributeStatement[] statements, 
			Set<String> allGroups, AttributesMapper mapper)
	{
		Map<String, Attribute<?>> collectedAttributes = new HashMap<String, Attribute<?>>();
		Map<String, Attribute<?>> attributesInGroup = allAttributesByGroup.get(group);
		if (attributesInGroup != null)
		{
			if (attribute == null)
				collectedAttributes.putAll(attributesInGroup);
			else
			{
				Attribute<?> at = attributesInGroup.get(attribute);
				if (at != null)
					collectedAttributes.put(attribute, at);
			}
		}
		
		for (AttributeStatement as: statements)
		{
			processAttributeStatement(mode, as, attribute, collectedAttributes, upwardsAttributesByGroup,
					downwardsAttributesByGroup, allGroups, mapper);
		}
		return collectedAttributes;
	}

	
	/**
	 * Checks all conditions. If all are true, then the attribute of the statement is added to the map.
	 * In case when the attribute is already in the map, conflict resolution of the statement is taken into 
	 * account.
	 * @param collectedAttributes
	 * @param statement
	 * @param mapper
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void processAttributeStatement(CollectionMode mode, AttributeStatement statement, String attribute, 
			Map<String, Attribute<?>> collectedAttributes, 
			Map<String, Map<String, Attribute<?>>> upwardsAttributesByGroup,
			Map<String, Map<String, Attribute<?>>> downwardsAttributesByGroup,
			Set<String> allGroups, AttributesMapper mapper)
	{
		if (!isForInterestingAttribute(attribute, statement))
			return;
		AttributeStatementCondition condition = statement.getCondition();
		if (!evaluateCondition(mode, condition, upwardsAttributesByGroup, 
					downwardsAttributesByGroup, allGroups))
				return;
		
		Attribute<?> ret = statement.getAssignedAttribute();
		if (collectedAttributes.containsKey(ret.getName()))
		{
			ConflictResolution resolution = statement.getConflictResolution();
			switch (resolution)
			{
			case skip:
				return;
			case overwrite:
				collectedAttributes.put(ret.getName(), ret);
				return;
			case merge:
				AttributeTypeBean atb = attrResolver.resolveAttributeType(ret.getName(), mapper);
				AttributeType at = attrResolver.resolveAttributeTypeBean(atb);
				if (at.getMaxElements() == Integer.MAX_VALUE)
				{
					Attribute<?> existing = collectedAttributes.get(ret.getName());
					((List)existing.getValues()).addAll(ret.getValues());
				}
				return;
			}
		} else
		{
			collectedAttributes.put(ret.getName(), ret);
		}
	}
	
	
	private boolean evaluateCondition(CollectionMode mode, AttributeStatementCondition condition, 
			Map<String, Map<String, Attribute<?>>> upwardsAttributesByGroup,
			Map<String, Map<String, Attribute<?>>> downwardsAttributesByGroup,
			Set<String> allGroups)
	{
		Type type = condition.getType();
		switch(type)
		{
		case everybody:
			return true;
		case hasParentgroupAttribute:
			if (mode == CollectionMode.downwards)
				return false;
			Attribute<?> pConditionAttr = condition.getAttribute();
			return hasAttributeInGroup(pConditionAttr.getGroupPath(), pConditionAttr.getName(), 
					upwardsAttributesByGroup);
		case hasSubgroupAttribute:
			if (mode == CollectionMode.upwards)
				return false;
			Attribute<?> sConditionAttr = condition.getAttribute();
			return hasAttributeInGroup(sConditionAttr.getGroupPath(), sConditionAttr.getName(), 
					downwardsAttributesByGroup);
		case hasParentgroupAttributeValue:
			if (mode == CollectionMode.downwards)
				return false;
			Attribute<?> pConditionAttr1 = condition.getAttribute();
			return hasAttributeAndValInGroup(pConditionAttr1, upwardsAttributesByGroup);
		case hasSubgroupAttributeValue:
			if (mode == CollectionMode.upwards)
				return false;
			Attribute<?> conditionAttr1 = condition.getAttribute();
			return hasAttributeAndValInGroup(conditionAttr1, downwardsAttributesByGroup);
		case memberOf:
			return allGroups.contains(condition.getGroup());
		default:
			throw new RuntimeEngineException("Unsupported condition: " + type);
		}
	}
	
	private boolean hasAttributeInGroup(String group, String attribute, 
			Map<String, Map<String, Attribute<?>>> allAttributesByGroup)
	{
		Map<String, Attribute<?>> attributesInGroup = allAttributesByGroup.get(group);
		return attributesInGroup != null && attributesInGroup.containsKey(attribute);
	}

	private boolean hasAttributeAndValInGroup(Attribute<?> attributeToSearch, 
			Map<String, Map<String, Attribute<?>>> allAttributesByGroup)
	{
		Map<String, Attribute<?>> attributesInGroup = allAttributesByGroup.get(attributeToSearch.getGroupPath());
		if (attributesInGroup == null)
			return false;
		Attribute<?> attribute = attributesInGroup.get(attributeToSearch.getName());
		if (attribute == null)
			return false;
		@SuppressWarnings("unchecked")
		AttributeValueSyntax<Object> syntax = (AttributeValueSyntax<Object>) attribute.getAttributeSyntax();
		for (Object val: attributeToSearch.getValues())
		{
			boolean found = false;
			for (Object val2: attribute.getValues())
			{
				if (syntax.areEqual(val, val2))
				{
					found = true;
					break;
				}
			}
			if (!found)
				return false;
		}
		return true;
	}
}
