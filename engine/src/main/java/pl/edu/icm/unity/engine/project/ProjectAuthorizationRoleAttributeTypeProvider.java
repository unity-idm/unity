/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.AbstractAttributeTypeProvider;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;


/**
 * Defines group authorization role attribute
 * @author P.Piernik
 */
@Component
public class ProjectAuthorizationRoleAttributeTypeProvider extends AbstractAttributeTypeProvider
{
	public static final String PROJECT_MANAGEMENT_AUTHORIZATION_ROLE = "sys:ProjectManagementRole";

	@Autowired
	public ProjectAuthorizationRoleAttributeTypeProvider(MessageSource msg)
	{
		super(msg);
	}

	@Override
	protected AttributeType getAttributeType()
	{
		Set<String> vals = Stream.of(GroupAuthorizationRole.values()).map(e -> e.toString())
				.collect(Collectors.toSet());

		EnumAttributeSyntax syntax = new EnumAttributeSyntax(vals);
		AttributeType authorizationAt = new AttributeType(PROJECT_MANAGEMENT_AUTHORIZATION_ROLE,
				EnumAttributeSyntax.ID, msg, PROJECT_MANAGEMENT_AUTHORIZATION_ROLE,
				new Object[] { getRolesDescription() });
		authorizationAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		authorizationAt.setMinElements(1);
		authorizationAt.setMaxElements(10);
		authorizationAt.setUniqueValues(true);
		authorizationAt.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
		return authorizationAt;
	}
	
	
	private String getRolesDescription()
	{
		StringBuilder ret = new StringBuilder();
		for (GroupAuthorizationRole role: GroupAuthorizationRole.values())
		{
			ret.append("<br><b>").append(role).append("</b> - ").
				append(role.getDescription()).append("\n");
		}
		return ret.toString();
	}
}
