/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.idp;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.SystemAttributesProvider;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeType;

@Component
public class IdPSystemAttributeTypeProvider implements SystemAttributesProvider
{
	public static final String LAST_ACCESS = "sys:idp:clientLastAccess";
	public final List<AttributeType> idpAttributes;

	private final MessageSource msg;

	@Autowired
	public IdPSystemAttributeTypeProvider(MessageSource msg)
	{
		this.msg = msg;
		idpAttributes = new ArrayList<>();
		idpAttributes.add(getLastAccessAttributeType());
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
		return idpAttributes;
	}

	@Override
	public boolean requiresUpdate(AttributeType at)
	{
		return false;
	}
}
