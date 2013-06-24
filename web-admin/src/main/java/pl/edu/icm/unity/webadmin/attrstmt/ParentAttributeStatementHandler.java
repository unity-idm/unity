/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attrstmt;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.attrstmnt.HasParentAttributeStatement;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Factory for Web UI code supporting {@link HasParentAttributeStatement}.  
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
public class ParentAttributeStatementHandler implements AttributeStatementWebHandlerFactory
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry handlersReg;
	
	
	@Autowired
	public ParentAttributeStatementHandler(UnityMessageSource msg,
			AttributeHandlerRegistry handlersReg)
	{
		this.msg = msg;
		this.handlersReg = handlersReg;
	}

	@Override
	public AttributeStatementComponent getEditorComponent(List<AttributeType> attributeTypes, String group)
	{
		return new ParentAttributeStatementComponent(msg, handlersReg, attributeTypes, group);
	}

	@Override
	public String getTextRepresentation(AttributeStatement as)
	{
		return StatementHandlerUtils.getTextRepresentation(msg, handlersReg, as, 
				"AttributeStatements.hasParentgroupAttribute");
	}

	@Override
	public String getSupportedAttributeStatementName()
	{
		return HasParentAttributeStatement.NAME;
	}
	
	public class ParentAttributeStatementComponent extends AbstractAttributeStatementComponent
	{
		public ParentAttributeStatementComponent(UnityMessageSource msg,
				AttributeHandlerRegistry attrHandlerRegistry,
				List<AttributeType> attributeTypes, String group)
		{
			super(msg, attrHandlerRegistry, attributeTypes, group,
					new HasParentAttributeStatement().getDescription());
			addAssignedAttributeField();
			addConditionAttributeField();
		}

		@Override
		public AttributeStatement getStatementFromComponent()
				throws FormValidationException
		{
			HasParentAttributeStatement ret = new HasParentAttributeStatement();
			ret.setAssignedAttribute(getAssignedAttribute());
			Attribute<?> condAttr = getConditionAttribute();
			condAttr.setGroupPath(new Group(group).getParentPath());
			ret.setConditionAttribute(condAttr);
			return ret;
		}

		@Override
		public void setInitialData(AttributeStatement initial)
		{
			setAssignedAttribute(initial.getAssignedAttribute());
			Attribute<?> condAttr = initial.getConditionAttribute();
			if (condAttr != null)
				setConditionAttribute(condAttr);
		}
	}
}
