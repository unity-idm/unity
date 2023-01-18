/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.AbstractAttributeTypeProvider;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeType;

@Component
public class ProjectManagerRestRoleAttributeTypeProvider extends AbstractAttributeTypeProvider
{
	public static final String AUTHORIZATION_ROLE = "sys:ProjectManagementRESTAPIRole";
	private InternalAuthorizationManager authz;

	@Autowired
	public ProjectManagerRestRoleAttributeTypeProvider(MessageSource msg, InternalAuthorizationManager authz)
	{
		super(msg);
		this.authz = authz;
	}
	
	@Override
	protected AttributeType getAttributeType()
	{
		EnumAttributeSyntax syntax = new EnumAttributeSyntax("manager");
		AttributeType authorizationAt = new AttributeType(AUTHORIZATION_ROLE, EnumAttributeSyntax.ID,
				msg, AUTHORIZATION_ROLE, new Object[] {authz.getRolesDescription()});
		authorizationAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		authorizationAt.setMinElements(1);
		authorizationAt.setMaxElements(1);
		authorizationAt.setUniqueValues(true);
		authorizationAt.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
		return authorizationAt;
	}
}
