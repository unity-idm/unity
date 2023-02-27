/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.rest;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.SystemAttributesProvider;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeType;

import java.util.List;

@Component
class ProjectManagerRestRoleAttributeTypeProvider implements SystemAttributesProvider
{
	static final String AUTHORIZATION_ROLE = "sys:ProjectManagementRESTAPIRole";
	private final MessageSource msg;

	@Autowired
	public ProjectManagerRestRoleAttributeTypeProvider(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public List<AttributeType> getSystemAttributes()
	{
		return Lists.newArrayList(getAttributeType());
	}

	@Override
	public boolean requiresUpdate(AttributeType at)
	{
		return false;
	}

	protected AttributeType getAttributeType()
	{
		EnumAttributeSyntax syntax = new EnumAttributeSyntax("manager");
		AttributeType authorizationAt = new AttributeType(AUTHORIZATION_ROLE, EnumAttributeSyntax.ID,
				msg, AUTHORIZATION_ROLE, new Object[] {"<b>manager</b> - Complete rest project management"});
		authorizationAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		authorizationAt.setMinElements(1);
		authorizationAt.setMaxElements(1);
		authorizationAt.setUniqueValues(true);
		authorizationAt.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
		return authorizationAt;
	}
}
