/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.enquiry;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.SystemAttributesProvider;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

/**
 * Defines string attribute types used to store user's filled and ignored enquires
 * @author K. Benedyczak
 */
@Component
public class EnquiryAttributeTypesProvider implements SystemAttributesProvider
{
	public static final String FILLED_ENQUIRES = "sys:FilledEnquires";
	public static final String IGNORED_ENQUIRES = "sys:IgnoredEnquires";
	
	private List<AttributeType> systemAttributes;
	
	@Autowired
	public EnquiryAttributeTypesProvider(MessageSource msg)
	{
		systemAttributes = Lists.newArrayList(getFilledEnquiresAT(msg),
				getIgnoredEnquiresAT(msg));
	}
	
	private AttributeType getFilledEnquiresAT(MessageSource msg)
	{
		AttributeType preferenceAt = new AttributeType(FILLED_ENQUIRES, StringAttributeSyntax.ID, msg);
		preferenceAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		preferenceAt.setMinElements(0);
		preferenceAt.setMaxElements(Integer.MAX_VALUE);
		preferenceAt.setUniqueValues(true);
		return preferenceAt;
	}

	private AttributeType getIgnoredEnquiresAT(MessageSource msg)
	{
		AttributeType preferenceAt = new AttributeType(IGNORED_ENQUIRES, StringAttributeSyntax.ID, msg);
		preferenceAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		preferenceAt.setMinElements(0);
		preferenceAt.setMaxElements(Integer.MAX_VALUE);
		preferenceAt.setUniqueValues(true);
		return preferenceAt;
	}

	@Override
	public List<AttributeType> getSystemAttributes()
	{
		return systemAttributes;
	}

	@Override
	public boolean requiresUpdate(AttributeType at)
	{
		return false;
	}
}
