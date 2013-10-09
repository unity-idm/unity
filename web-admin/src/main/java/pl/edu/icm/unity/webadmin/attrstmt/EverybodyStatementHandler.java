/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attrstmt;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.attrstmnt.EverybodyStatement;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Factory for Web UI code supporting {@link EverybodyStatement}.  
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
public class EverybodyStatementHandler implements AttributeStatementWebHandlerFactory
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry handlersReg;

	
	@Autowired
	public EverybodyStatementHandler(UnityMessageSource msg, AttributeHandlerRegistry handlersReg)
	{
		this.msg = msg;
		this.handlersReg = handlersReg;
	}

	@Override
	public AttributeStatementComponent getEditorComponent(Collection<AttributeType> attributeTypes, String group)
	{
		return new EverybodyStatementComponent(msg, handlersReg, attributeTypes, group);
	}
	
	@Override
	public String getTextRepresentation(AttributeStatement as)
	{
		StringBuilder sb = StatementHandlerUtils.getAssignedAttributeText(msg, handlersReg, as);
		sb.append(msg.getMessage("AttributeStatements.everybody"));
		return sb.toString();
	}

	@Override
	public String getSupportedAttributeStatementName()
	{
		return EverybodyStatement.NAME;
	}
	
	public static class EverybodyStatementComponent extends AbstractAttributeStatementComponent
	{
		public EverybodyStatementComponent(UnityMessageSource msg,
				AttributeHandlerRegistry attrHandlerRegistry,
				Collection<AttributeType> attributeTypes, String group)
		{
			super(msg, attrHandlerRegistry, attributeTypes, group, new EverybodyStatement().getDescription());
			addAssignedAttributeField();
		}

		@Override
		public AttributeStatement getStatementFromComponent()
				throws FormValidationException
		{
			EverybodyStatement ret = new EverybodyStatement();
			ret.setAssignedAttribute(getAssignedAttribute());
			return ret;
		}

		@Override
		public void setInitialData(AttributeStatement initial)
		{
			setAssignedAttribute(initial.getAssignedAttribute());
		}
	}
}
