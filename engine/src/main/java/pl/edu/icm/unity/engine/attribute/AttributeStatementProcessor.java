/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeClassHelper;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeStatement.Direction;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Immutable class handling group attribute statements.
 * @author K. Benedyczak
 */
@Component
public class AttributeStatementProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, AttributeStatementProcessor.class);

	public enum ContextKey
	{
		idsByType,
		attrs,
		attr,
		eattrs,
		eattr,
		groupName,
		groups,
		entityId;
	}
	
	private AttributeTypeHelper atHelper;
	private AttributeValueConverter attrConverter;

	
	@Autowired
	public AttributeStatementProcessor(AttributeTypeHelper atHelper, AttributeValueConverter attrConverter)
	{
		this.atHelper = atHelper;
		this.attrConverter = attrConverter;
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
	 * @throws WrongArgumentException 
	 */
	public Map<String, AttributeExt> getEffectiveAttributes(List<Identity> identities, String group, 
			String queriedAttribute, 
			Set<String> allGroups, Map<String, Map<String, AttributeExt>> directAttributesByGroup,
			Map<String, AttributesClass> knownClasses,
			Function<String, Group> groupInfoProvider,
			Function<String, AttributeType> attrTypeProvider) 
	{		
		Map<String, Map<String, AttributeExt>> downwardsAttributes = new HashMap<>();
		collectUpOrDownAttributes(Direction.downwards, group, null, identities, downwardsAttributes, 
				directAttributesByGroup, allGroups, knownClasses, groupInfoProvider, attrTypeProvider);

		Map<String, Map<String, AttributeExt>> upwardsAttributes = new HashMap<>();
		collectUpOrDownAttributes(Direction.upwards, group, null, identities, upwardsAttributes, 
				directAttributesByGroup, allGroups, knownClasses, groupInfoProvider, attrTypeProvider);

		AttributeStatement[] statements = getGroupStatements(group, groupInfoProvider);
		
		return processAttributeStatements(Direction.undirected, directAttributesByGroup, 
				upwardsAttributes, downwardsAttributes, group, 
				queriedAttribute, identities, statements, allGroups, knownClasses, attrTypeProvider);
	}

	/**
	 * Resolves group path and returns group's attribute statements
	 * @param groupPath
	 * @param mapper
	 * @param gMapper
	 * @return 
	 * @throws IllegalGroupValueException 
	 */
	private AttributeStatement[] getGroupStatements(String groupPath, Function<String, Group> groupInfoProvider) 
	{
		return groupInfoProvider.apply(groupPath).getAttributeStatements();
	}
	
	/**
	 * Recursive method collecting attributes in down or up direction. Works as follows:
	 * <ol>
	 * <li> statements of the group are established
	 * <li> for each statement which has condition related to an attribute in a other group in the 
	 * direction of the mode, the group is recorded to a set.
	 * <li> for each group from the set recursive call is made
	 * <li> normal processing of the statements of this group is performed, however only the input
	 * for rules related to the groups in the mode is provided. Statements in opposite direction are ignored. 
	 * </ol>
	 */
	private void collectUpOrDownAttributes(Direction mode, String groupPath, String queriedAttribute, 
			List<Identity> identities,
			Map<String, Map<String, AttributeExt>> upOrDownAttributes, 
			Map<String, Map<String, AttributeExt>> allAttributesByGroup,
			Set<String> allGroups, Map<String, AttributesClass> knownClasses,
			Function<String, Group> groupInfoProvider,
			Function<String, AttributeType> attrTypeProvider) 
	{
		AttributeStatement[] statements = getGroupStatements(groupPath, groupInfoProvider);
		
		Set<String> interestingGroups = new HashSet<String>();
		for (AttributeStatement as: statements)
		{
			if (as.isSuitableForDirectedEvaluation(mode, groupPath) 
					&& isForInterestingAttribute(queriedAttribute, as))
			{
				String groupPath2 = as.getExtraAttributesGroup();
				if (groupPath2 != null)
					interestingGroups.add(groupPath2);
			}
		}
		for (String interestingGroup: interestingGroups)
		{
			if (!allGroups.contains(interestingGroup))
				continue;
			collectUpOrDownAttributes(mode, interestingGroup, queriedAttribute, identities,
					upOrDownAttributes, allAttributesByGroup,
					allGroups, knownClasses, groupInfoProvider, attrTypeProvider);
		}
		
		Map<String, AttributeExt> ret = (mode == Direction.upwards) ? 
				processAttributeStatements(mode, allAttributesByGroup, upOrDownAttributes, null,
						groupPath, null, identities, statements, allGroups, knownClasses, attrTypeProvider):
				processAttributeStatements(mode, allAttributesByGroup, null, upOrDownAttributes, 
						groupPath, null, identities, statements, allGroups, knownClasses, attrTypeProvider);
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
	
	private Map<String, AttributeExt> processAttributeStatements(Direction direction,
			Map<String, Map<String, AttributeExt>> allRegularAttributesByGroup,
			Map<String, Map<String, AttributeExt>> upwardsAttributesByGroup,
			Map<String, Map<String, AttributeExt>> downwardsAttributesByGroup,
			String group, String queriedAttribute, List<Identity> identities, AttributeStatement[] statements, 
			Set<String> allGroups, Map<String, AttributesClass> knownClasses,
			Function<String, AttributeType> attrTypeProvider) 
	{
		Map<String, AttributeExt> collectedAttributes = new HashMap<String, AttributeExt>();
		Map<String, AttributeExt> regularAttributesInGroup = allRegularAttributesByGroup.get(group);
		if (regularAttributesInGroup == null)
			regularAttributesInGroup = new HashMap<String, AttributeExt>();
		AttributeExt acAttribute = null;
		if (regularAttributesInGroup != null)
		{
			if (queriedAttribute == null)
			{
				for (Map.Entry<String, AttributeExt> a: regularAttributesInGroup.entrySet())
					collectedAttributes.put(a.getKey(), new AttributeExt(a.getValue()));
			} else
			{
				AttributeExt at = regularAttributesInGroup.get(queriedAttribute);
				if (at != null)
					collectedAttributes.put(queriedAttribute, new AttributeExt(at));
			}
			acAttribute = (AttributeExt) regularAttributesInGroup.get(
					AttributeClassUtil.ATTRIBUTE_CLASSES_ATTRIBUTE);
		}

		AttributeClassHelper acHelper = acAttribute == null ? new AttributeClassHelper() :
			new AttributeClassHelper(knownClasses, acAttribute.getValues());
		
		int i=1;
		for (AttributeStatement as: statements)
		{
			Map<String, AttributeExt> extraAttributes = null;
			String extraAttributesGroup = as.getExtraAttributesGroup();
			if (extraAttributesGroup != null)
			{
				if (extraAttributesGroup.startsWith(group) && downwardsAttributesByGroup != null)
					extraAttributes = downwardsAttributesByGroup.get(extraAttributesGroup);
				if (group.startsWith(extraAttributesGroup) && upwardsAttributesByGroup != null)
					extraAttributes = upwardsAttributesByGroup.get(extraAttributesGroup);
			}
			
			ThreadContext.push("[AttrStmnt " + i + " in " + group + " for entity " + 
				identities.get(0).getEntityId() + "]");
			try
			{
				processAttributeStatement(direction, group, as, queriedAttribute, identities, 
						collectedAttributes, regularAttributesInGroup, extraAttributes, 
						allGroups, acHelper, attrTypeProvider);
			} catch (Exception e) 
			{
				log.error("Error processing statement " + 
						as + " is skipped", e);
			} finally
			{
				ThreadContext.pop();
			}
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
	private void processAttributeStatement(Direction direction, String group, AttributeStatement statement, 
			String attribute, List<Identity> identities,
			Map<String, AttributeExt> collectedAttributes,
			Map<String, AttributeExt> regularGroupAttributes,
			Map<String, AttributeExt> extraGroupAttributes,
			Set<String> allGroups, AttributeClassHelper acHelper,
			Function<String, AttributeType> attrTypeProvider) 
	{
		//we are in the recursive process of establishing downwards or upwards attributes and the
		// statement is oppositely directed. 
		if (direction != Direction.undirected && !statement.isSuitableForDirectedEvaluation(direction, group))
			return;

		if (!acHelper.isAllowed(attribute))
			return;

		if (!isForInterestingAttribute(attribute, statement))
			return;
		
		Map<String, Object> context = createMvelContext(allGroups, group, identities, 
				regularGroupAttributes, extraGroupAttributes);
		
		boolean condition = evaluateStatementCondition(statement, context);
		if (!condition)
			return;
		
		Attribute ret = statement.dynamicAttributeMode() ? 
				evaluateStatementValue(statement, group, context, attrTypeProvider) : 
				statement.getFixedAttribute();
		
		if (ret == null)
			return;
		
		AttributeExt existing = collectedAttributes.get(ret.getName());
		if (existing != null)
		{
			ConflictResolution resolution = statement.getConflictResolution();
			switch (resolution)
			{
			case skip:
				log.trace("Conflict detected, skipping dynamic attribute");
				return;
			case overwrite:
				if (!existing.isDirect())
				{
					log.trace("Conflict detected, overwritting the previous dynamic attribute");
					collectedAttributes.put(ret.getName(), new AttributeExt(ret, false));
				} else
				{
					log.trace("Conflict detected, NOT overwritting the existing regular attribute");					
				}
				return;
			case merge:
				log.trace("Conflict detected, will try to merge values");					
				AttributeType at = attrTypeProvider.apply(ret.getName());
				if (at.getMaxElements() == Integer.MAX_VALUE)
				{
					((List)existing.getValues()).addAll(ret.getValues());
					log.trace("Merge of values was performed");					
				} else
				{
					log.trace("Merge of values was skipped as "
							+ "attribute type has a values number limit");
				}
				return;
			}
		} else
		{
			collectedAttributes.put(ret.getName(), new AttributeExt(ret, false));
		}
	}
	
	
	private boolean evaluateStatementCondition(AttributeStatement statement, Map<String, Object> context)
	{
		Boolean result = null;
		try
		{
			result = (Boolean) MVEL.executeExpression(statement.getCompiledCondition(), context, 
					new HashMap<>());
		} catch (Exception e)
		{
			log.warn("Error during attribute statement condition evaluation, condition '"
					+ statement.getCondition() + "' is invalid. Skipping statement.\n" 
					+ e.toString());
			if (log.isTraceEnabled())
				log.trace("Full stack trace of the problematic attribute statement error", e);
			return false;
		}
		
		if (result == null)
		{
			log.debug("Condition evaluated to null value, assuming false");
			return false;
		}
		if (log.isTraceEnabled())
		{
			log.trace("Condition " + statement.getCondition() + " evaluated to " + 
					result + " for " + context.get(ContextKey.entityId.name()) + " in " 
					+ context.get(ContextKey.groupName.name()));
		}
		return result.booleanValue();
	}

	private Attribute evaluateStatementValue(AttributeStatement statement, 
			String group, Map<String, Object> context, Function<String, AttributeType> attrTypeProvider)
	{
		Object value;
		try
		{
			value = MVEL.executeExpression(statement.getCompiledDynamicAttributeExpression(), context,
				new HashMap<>());
		} catch (Exception e)
		{
			log.warn("Error during attribute statement value evaluation, expression '"
					+ statement.getDynamicAttributeExpression() + 
					"' is invalid. Skipping statement.\n" + e.toString());
			if (log.isTraceEnabled())
				log.trace("Full stack trace of the problematic attribute statement error", e);
			return null;
		}
		
		if (value == null)
		{
			log.debug("Attribute value evaluated to null, skipping");
			return null;
		}
		
		String attributeName = statement.getDynamicAttributeType();
		AttributeType attributeType = attrTypeProvider.apply(attributeName);
		AttributeValueSyntax<?> valueSyntax = atHelper.getSyntax(attributeType);
		
		List<String> typedValues;
		try
		{
			typedValues = convertValues(value, valueSyntax);
		} catch (IllegalAttributeValueException e)
		{
			log.debug("Can't convert attribute values returned by the statement's expression "
					+ "to the type of attribute " + attributeName + ", skipping it", e);
			return null;
		}
		
		Attribute attribute = new Attribute(attributeName, valueSyntax.getValueSyntaxId(), group, typedValues);

		if (log.isTraceEnabled())
		{
			log.trace("Evaluated values of attribute " + attributeName + ":" +
					typedValues);
		}
		return attribute;
	}


	public static <T> List<String> convertValues(Object value, AttributeValueSyntax<T> syntax) 
			throws IllegalAttributeValueException
	{
		List<?> aValues = value instanceof List ? (List<?>)value : Collections.singletonList(value);
		List<String> ret = new ArrayList<>(aValues.size());
		for (Object o: aValues)
		{
			T converted = syntax.deserializeSimple(o.toString());
			String converted2 = syntax.convertToString(converted);
			ret.add(converted2);
		}
		return ret;
	}

	private Map<String, Object> createMvelContext(Set<String> allGroups, String groupName, 
			List<Identity> identities,
			Map<String, AttributeExt> directAttributes, Map<String, AttributeExt> extraAttributes)
	{
		Map<String, Object> ret = new HashMap<>();
		
		ret.put(ContextKey.entityId.name(), identities.get(0).getEntityId());
		ret.put(ContextKey.groupName.name(), groupName);
		ret.put(ContextKey.groups.name(), new ArrayList<String>(allGroups));

		Map<String, List<String>> idsByType = new HashMap<String, List<String>>();
		for (Identity id: identities)
		{
			List<String> vals = idsByType.get(id.getTypeId());
			if (vals == null)
			{
				vals = new ArrayList<String>();
				idsByType.put(id.getTypeId(), vals);
			}
			vals.add(id.getValue());
		}
		ret.put(ContextKey.idsByType.name(), idsByType);
		
		addAttributesToContext(directAttributes, ContextKey.attrs, ContextKey.attr, ret);
		if (extraAttributes != null)
		{
			addAttributesToContext(extraAttributes, ContextKey.eattrs, ContextKey.eattr, ret);
		} else
		{
			ret.put(ContextKey.eattrs.name(), null);
			ret.put(ContextKey.eattr.name(), null);
		}
		
		return ret;
	}
	
	private void addAttributesToContext(Map<String, AttributeExt> attributes, ContextKey fullKey,
			ContextKey oneValueKey, Map<String, Object> target)
	{
		Map<String, Object> attr = new HashMap<>();
		Map<String, List<String>> attrs = new HashMap<>();
		for (AttributeExt a: attributes.values())
		{
			List<String> values = internalValuesToExternal(a.getValueSyntax(), a.getValues());	
			String v = values.isEmpty() ? "" : values.get(0);
			attr.put(a.getName(), v);
			attrs.put(a.getName(), values);
		}
		target.put(oneValueKey.name(), attr);
		target.put(fullKey.name(), attrs);
	}

	private List<String> internalValuesToExternal(String valueSyntax, List<String> internalValues) 
	{
		AttributeValueSyntax<?> syntax = atHelper.getUnconfiguredSyntax(valueSyntax);
		return attrConverter.internalValuesToExternal(syntax, internalValues);
	}
}
