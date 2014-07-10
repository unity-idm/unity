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

import pl.edu.icm.unity.db.generic.ac.AttributeClassUtil;
import pl.edu.icm.unity.db.json.GroupsSerializer;
import pl.edu.icm.unity.db.mapper.AttributesMapper;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.AttributeTypeBean;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.resolvers.AttributesResolver;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.attributes.AttributeClassHelper;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.Direction;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Group;

/**
 * Immutable class handling group attribute statements.
 * @author K. Benedyczak
 */
@Component
public class AttributeStatementProcessor
{
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
	 * @param queriedAttribute the only interesting attribute or null if all should be collected
	 * @param allGroups set with all groups where the entity is the member
	 * @param directAttributesByGroup map with group as keys with all regular attributes of the user. Values
	 * are maps of attributes by name.
	 * @param atMapper
	 * @param gMapper
	 * @return collected attributes in a map form. Map keys are attribute names.
	 * @throws IllegalGroupValueException 
	 * @throws WrongArgumentException 
	 */
	public Map<String, AttributeExt<?>> getEffectiveAttributes(long entityId, String group, String queriedAttribute, 
			Set<String> allGroups, Map<String, Map<String, AttributeExt<?>>> directAttributesByGroup,
			AttributesMapper atMapper, GroupsMapper gMapper, Map<String, AttributesClass> knownClasses) 
					throws IllegalGroupValueException, IllegalTypeException
	{		
		Map<String, Map<String, AttributeExt<?>>> downwardsAttributes = new HashMap<String, Map<String,AttributeExt<?>>>();
		collectUpOrDownAttributes(Direction.downwards, group, queriedAttribute, downwardsAttributes, 
				directAttributesByGroup, allGroups, atMapper, gMapper, knownClasses);

		Map<String, Map<String, AttributeExt<?>>> upwardsAttributes = new HashMap<String, Map<String,AttributeExt<?>>>();
		collectUpOrDownAttributes(Direction.upwards, group, queriedAttribute, upwardsAttributes, 
				directAttributesByGroup, allGroups, atMapper, gMapper, knownClasses);

		AttributeStatement[] statements = getGroupStatements(group, atMapper, gMapper);
		
		return processAttributeStatements(Direction.undirected, directAttributesByGroup, 
				upwardsAttributes, downwardsAttributes, group, 
				queriedAttribute, statements, allGroups, atMapper, knownClasses);
	}

	/**
	 * Resolves group path and returns group's attribute statements
	 * @param groupPath
	 * @param mapper
	 * @param gMapper
	 * @return 
	 * @throws IllegalGroupValueException 
	 */
	private AttributeStatement[] getGroupStatements(String groupPath, AttributesMapper mapper, 
			GroupsMapper gMapper) throws IllegalGroupValueException
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
	 * @throws IllegalGroupValueException 
	 * @throws WrongArgumentException 
	 * @throws IllegalAttributeTypeException 
	 * @throws IllegalTypeException 
	 */
	private void collectUpOrDownAttributes(Direction mode, String groupPath, String queriedAttribute,
			Map<String, Map<String, AttributeExt<?>>> upOrDownAttributes, 
			Map<String, Map<String, AttributeExt<?>>> allAttributesByGroup,
			Set<String> allGroups, AttributesMapper mapper, GroupsMapper gMapper, 
			Map<String, AttributesClass> knownClasses) 
			throws IllegalGroupValueException, IllegalTypeException
	{
		AttributeStatement[] statements = getGroupStatements(groupPath, mapper, gMapper);
		
		Set<String> interestingGroups = new HashSet<String>();
		for (AttributeStatement as: statements)
		{
			if (mode == as.getDirection() && isForInterestingAttribute(queriedAttribute, as))
			{
				String groupPath2 = as.getConditionAttribute().getGroupPath();
				interestingGroups.add(groupPath2);
			}
		}
		for (String interestingGroup: interestingGroups)
		{
			collectUpOrDownAttributes(mode, interestingGroup, queriedAttribute, upOrDownAttributes, allAttributesByGroup,
					allGroups, mapper, gMapper, knownClasses);
		}
		
		Map<String, AttributeExt<?>> ret = (mode == Direction.upwards) ? 
				processAttributeStatements(mode, allAttributesByGroup, upOrDownAttributes, null,
						groupPath, null, statements, allGroups, mapper, knownClasses):
				processAttributeStatements(mode, allAttributesByGroup, null, upOrDownAttributes, 
						groupPath, null, statements, allGroups, mapper, knownClasses);
		upOrDownAttributes.put(groupPath, ret);
	}
	
	private boolean isForInterestingAttribute(String attribute, AttributeStatement as)
	{
		if (attribute == null)
			return true;
		String assigned = as.getAssignedAttributeName();
		if (assigned == null || assigned.equals(attribute))
			return true;
		return false;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<String, AttributeExt<?>> processAttributeStatements(Direction direction,
			Map<String, Map<String, AttributeExt<?>>> allAttributesByGroup,
			Map<String, Map<String, AttributeExt<?>>> upwardsAttributesByGroup,
			Map<String, Map<String, AttributeExt<?>>> downwardsAttributesByGroup,
			String group, String queriedAttribute, AttributeStatement[] statements, 
			Set<String> allGroups, AttributesMapper mapper, Map<String, AttributesClass> knownClasses) 
					throws IllegalTypeException 
	{
		Map<String, AttributeExt<?>> collectedAttributes = new HashMap<String, AttributeExt<?>>();
		Map<String, AttributeExt<?>> attributesInGroup = allAttributesByGroup.get(group);
		AttributeExt<String> acAttribute = null;
		if (attributesInGroup != null)
		{
			if (queriedAttribute == null)
			{
				for (Map.Entry<String, AttributeExt<?>> a: attributesInGroup.entrySet())
					collectedAttributes.put(a.getKey(), new AttributeExt(a.getValue()));
			} else
			{
				AttributeExt<?> at = attributesInGroup.get(queriedAttribute);
				if (at != null)
					collectedAttributes.put(queriedAttribute, new AttributeExt(at));
			}
			acAttribute = (AttributeExt<String>) attributesInGroup.get(
					AttributeClassUtil.ATTRIBUTE_CLASSES_ATTRIBUTE);
		}

		AttributeClassHelper acHelper = acAttribute == null ? new AttributeClassHelper() :
			new AttributeClassHelper(knownClasses, acAttribute.getValues());
		
		for (AttributeStatement as: statements)
		{
			Map<String, Map<String, AttributeExt<?>>> directedAttributesByGroup = null;
			if (as.getDirection() == Direction.downwards)
				directedAttributesByGroup = downwardsAttributesByGroup;
			if (as.getDirection() == Direction.upwards)
				directedAttributesByGroup = upwardsAttributesByGroup;
			processAttributeStatement(direction, as, queriedAttribute, collectedAttributes, 
					directedAttributesByGroup, allGroups, mapper, acHelper);
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
	 * @throws IllegalTypeException 
	 * @throws IllegalAttributeTypeException 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void processAttributeStatement(Direction direction, AttributeStatement statement, String attribute, 
			Map<String, AttributeExt<?>> collectedAttributes, 
			Map<String, Map<String, AttributeExt<?>>> directedAttributesByGroup,
			Set<String> allGroups, AttributesMapper mapper, AttributeClassHelper acHelper) 
	{
		//we are in the recursive process of establishing downwards or upwards attributes and the
		// statement is oppositely directed. 
		if (direction != Direction.undirected && statement.getDirection() != Direction.undirected &&
				direction != statement.getDirection())
			return;

		if (!acHelper.isAllowed(attribute))
			return;

		if (!isForInterestingAttribute(attribute, statement))
			return;
		
		Attribute<?> ret = statement.evaluateCondition(directedAttributesByGroup, allGroups);
		
		if (ret == null)
			return;
		
		AttributeExt<?> existing = collectedAttributes.get(ret.getName());
		if (existing != null)
		{
			ConflictResolution resolution = statement.getConflictResolution();
			switch (resolution)
			{
			case skip:
				return;
			case overwrite:
				if (!existing.isDirect())
					collectedAttributes.put(ret.getName(), new AttributeExt(ret, false));
				return;
			case merge:
				try
				{
					AttributeTypeBean atb = attrResolver.resolveAttributeType(ret.getName(), mapper);
					AttributeType at = attrResolver.resolveAttributeTypeBean(atb);
					if (at.getMaxElements() == Integer.MAX_VALUE)
					{
						((List)existing.getValues()).addAll(ret.getValues());
					}
				} catch (EngineException e)
				{
					//OK, shouldn't happen, anyway ignore.
				}
				return;
			}
		} else
		{
			collectedAttributes.put(ret.getName(), new AttributeExt(ret, false));
		}
	}
}
