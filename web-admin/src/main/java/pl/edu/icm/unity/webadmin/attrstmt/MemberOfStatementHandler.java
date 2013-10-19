/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attrstmt;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.attrstmnt.MemberOfStatement;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GroupComboBox;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Factory for Web UI code supporting {@link MemberOfStatement}.  
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
public class MemberOfStatementHandler implements AttributeStatementWebHandlerFactory
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry handlersReg;
	private GroupsManagement groupsMan;
	
	
	@Autowired
	public MemberOfStatementHandler(UnityMessageSource msg,
			AttributeHandlerRegistry handlersReg, GroupsManagement groupsMan)
	{
		this.msg = msg;
		this.handlersReg = handlersReg;
		this.groupsMan = groupsMan;
	}

	@Override
	public AttributeStatementComponent getEditorComponent(Collection<AttributeType> attributeTypes, String group)
	{
		return new MemberOfStatementComponent(msg, handlersReg, attributeTypes, group);
	}

	@Override
	public String getTextRepresentation(AttributeStatement as)
	{
		StringBuilder sb = StatementHandlerUtils.getAssignedAttributeText(msg, handlersReg, as);
		sb.append(msg.getMessage("AttributeStatements.memberOf")).append(" ").append(as.getConditionGroup());
		return sb.toString();
	}

	@Override
	public String getSupportedAttributeStatementName()
	{
		return MemberOfStatement.NAME;
	}
	
	public class MemberOfStatementComponent extends AbstractAttributeStatementComponent
	{
		private GroupComboBox conditionAnyGroup;
		
		public MemberOfStatementComponent(UnityMessageSource msg,
				AttributeHandlerRegistry attrHandlerRegistry,
				Collection<AttributeType> attributeTypes, String group)
		{
			super(msg, attrHandlerRegistry, attributeTypes, group,
					new MemberOfStatement().getDescription());
			addAssignedAttributeField();
			conditionAnyGroup = new GroupComboBox(msg.getMessage("AttributeStatementEditDialog.group"), 
					groupsMan);
			conditionAnyGroup.setInput("/", true);
			main.addComponent(conditionAnyGroup);
		}

		@Override
		public AttributeStatement getStatementFromComponent()
				throws FormValidationException
		{
			MemberOfStatement ret = new MemberOfStatement();
			ret.setAssignedAttribute(getAssignedAttribute());
			ret.setConditionGroup((String) conditionAnyGroup.getValue());
			return ret;
		}

		@Override
		public void setInitialData(AttributeStatement initial)
		{
			setAssignedAttribute(initial.getAssignedAttribute());
			if (initial.getConditionGroup() != null)
				conditionAnyGroup.setValue(initial.getConditionGroup());
		}
	}
}
