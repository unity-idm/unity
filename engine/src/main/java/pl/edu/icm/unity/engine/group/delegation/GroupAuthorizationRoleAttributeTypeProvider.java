/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.group.delegation;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.AbstractAttributeTypeProvider;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.delegatedgroup.GroupAuthorizationRole;


/**
 * Defines group authorization role attribute
 * @author P.Piernik
 *
 */

@Component
public class GroupAuthorizationRoleAttributeTypeProvider extends AbstractAttributeTypeProvider
{
	public static final String GROUP_AUTHORIZATION_ROLE = "sys:GroupAuthorizationRole";

	@Autowired
	public GroupAuthorizationRoleAttributeTypeProvider(UnityMessageSource msg)
	{
		super(msg);
	}

	@Override
	protected AttributeType getAttributeType()
	{
		Set<String> vals = Stream.of(GroupAuthorizationRole.values()).map(e -> e.toString())
				.collect(Collectors.toSet());

		EnumAttributeSyntax syntax = new EnumAttributeSyntax(vals);
		AttributeType authorizationAt = new AttributeType(GROUP_AUTHORIZATION_ROLE,
				EnumAttributeSyntax.ID, msg, GROUP_AUTHORIZATION_ROLE,
				new Object[] { Stream.of(GroupAuthorizationRole.values())
						.map(e -> e.getDescription())
						.collect(Collectors.toSet()) });
		authorizationAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		authorizationAt.setMinElements(1);
		authorizationAt.setMaxElements(10);
		authorizationAt.setUniqueValues(true);
		authorizationAt.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
		return authorizationAt;
	}
}
