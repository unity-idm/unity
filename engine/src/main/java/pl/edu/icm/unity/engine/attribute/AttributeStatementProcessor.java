/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.engine.api.mvel.MVELGroup;
import pl.edu.icm.unity.types.basic.AttributeExt;
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
	 * @param queriedAttribute the only interesting attribute or null if all should be collected
	 * @param allGroups set with all groups where the entity is the member
	 * @param directAttributesByGroup map with group as keys with all regular attributes of the user. Values
	 * are maps of attributes by name.
	 * @return collected attributes in a map form. Map keys are attribute names.
	 */
	public Map<String, AttributeExt> getEffectiveAttributes(List<Identity> identities, String group, 
			String queriedAttribute, 
			Collection<Group> allGroups, 
			Map<String, Map<String, AttributeExt>> directAttributesByGroup,
			Map<String, AttributesClass> knownClasses,
			Function<String, Group> groupInfoProvider,
			Function<String, AttributeType> attrTypeProvider,
			Function<String, MVELGroup> mvelGroupProvider) 
	{		
		EffectiveAttributesEvaluator evaluator = new EffectiveAttributesEvaluator(atHelper, attrConverter, 
				identities, allGroups, directAttributesByGroup, knownClasses, 
				groupInfoProvider, attrTypeProvider, mvelGroupProvider);
		return evaluator.getEffectiveAttributes(group, queriedAttribute);
	}
}
