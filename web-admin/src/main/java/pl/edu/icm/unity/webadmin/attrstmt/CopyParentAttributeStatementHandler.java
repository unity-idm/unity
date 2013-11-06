/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attrstmt;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.attrstmnt.CopyParentAttributeStatement;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Factory for Web UI code supporting {@link CopyParentAttributeStatement}.  
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
public class CopyParentAttributeStatementHandler implements AttributeStatementWebHandlerFactory
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry handlersReg;
	
	
	@Autowired
	public CopyParentAttributeStatementHandler(UnityMessageSource msg,
			AttributeHandlerRegistry handlersReg)
	{
		this.msg = msg;
		this.handlersReg = handlersReg;
	}

	@Override
	public AttributeStatementComponent getEditorComponent(Collection<AttributeType> attributeTypes, String group)
	{
		return new CopyParentAttributeStatementComponent(msg, handlersReg, attributeTypes, group);
	}

	@Override
	public String getTextRepresentation(AttributeStatement as)
	{
		StringBuilder sb = new StringBuilder();
		Attribute<?> a = as.getConditionAttribute();
		String condAttrStr = handlersReg.getSimplifiedAttributeRepresentation(a,
				StatementHandlerUtils.ATTR_LEN);
		sb.append(msg.getMessage("AttributeStatements.copyParentAttribute")).append(" ").append(condAttrStr);
		return sb.toString();
	}

	@Override
	public String getSupportedAttributeStatementName()
	{
		return CopyParentAttributeStatement.NAME;
	}
	
	public class CopyParentAttributeStatementComponent extends AbstractAttributeStatementComponent
	{
		public CopyParentAttributeStatementComponent(UnityMessageSource msg,
				AttributeHandlerRegistry attrHandlerRegistry,
				Collection<AttributeType> attributeTypes, String group)
		{
			super(msg, attrHandlerRegistry, attributeTypes, group, 
					new CopyParentAttributeStatement().getDescription());
			addConditionAttributeField(false);
		}

		@Override
		public AttributeStatement getStatementFromComponent()
				throws FormValidationException
		{
			CopyParentAttributeStatement ret = new CopyParentAttributeStatement();
			Attribute<?> condAttr = getConditionAttribute();
			condAttr.setGroupPath(new Group(group).getParentPath());
			ret.setConditionAttribute(condAttr);
			return ret;
		}

		@Override
		public void setInitialData(AttributeStatement initial)
		{
			Attribute<?> condAttr = initial.getConditionAttribute();
			if (condAttr != null)
				setConditionAttribute(condAttr);
		}
	}
}
