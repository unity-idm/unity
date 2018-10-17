/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms;

import java.util.List;

import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;

/**
 * Utilities related to parsing a form
 * 
 * @author K. Benedyczak
 */
public class FormParser
{
	static boolean isGroupParamUsedAsMandatoryAttributeGroup(BaseForm form, GroupRegistrationParam groupParam)
	{
		return isGroupParamUsedAsMandatoryAttributeGroup(form.getAttributeParams(), groupParam);
	}
	
	static boolean isGroupParamUsedAsMandatoryAttributeGroup(List<AttributeRegistrationParam> attributeParams, 
			GroupRegistrationParam groupParam)
	{
		for (AttributeRegistrationParam attr: attributeParams)
		{
			if (attr.isUsingDynamicGroup() 
					&& attr.getDynamicGroup().equals(groupParam.getGroupPath()) 
					&& !attr.isOptional())
				return true;
		}
		return false;
	}
	
}
