/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.SystemAttributesProvider;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeType;

@Component
public class SAMLSystemAttributeTypeProvider implements SystemAttributesProvider
{
	public static final String LAST_ACCESS = "sys:saml:lastAccess";
	private List<AttributeType> samlAttributes = new ArrayList<AttributeType>();

	private final MessageSource msg;

	@Autowired
	public SAMLSystemAttributeTypeProvider(MessageSource msg)
	{
		this.msg = msg;
		samlAttributes.add(getLastAccessAttributeType());
	}

	private AttributeType getLastAccessAttributeType()
	{
		AttributeType preferenceAt = new AttributeType(LAST_ACCESS, StringAttributeSyntax.ID, msg);
		preferenceAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		preferenceAt.setMinElements(1);
		preferenceAt.setMaxElements(1);
		preferenceAt.setUniqueValues(false);
		return preferenceAt;
	}

	@Override
	public List<AttributeType> getSystemAttributes()
	{
		return samlAttributes;
	}

	@Override
	public boolean requiresUpdate(AttributeType at)
	{
		return false;
	}
}
