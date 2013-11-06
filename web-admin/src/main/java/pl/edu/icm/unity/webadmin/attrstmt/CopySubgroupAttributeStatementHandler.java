/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attrstmt;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.attrstmnt.CopySubgroupAttributeStatement;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GroupComboBox;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Factory for Web UI code supporting {@link CopySubgroupAttributeStatement}.  
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
public class CopySubgroupAttributeStatementHandler implements AttributeStatementWebHandlerFactory
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry handlersReg;
	private GroupsManagement groupsMan;
	
	
	@Autowired
	public CopySubgroupAttributeStatementHandler(UnityMessageSource msg,
			AttributeHandlerRegistry handlersReg, GroupsManagement groupsMan)
	{
		this.msg = msg;
		this.handlersReg = handlersReg;
		this.groupsMan = groupsMan;
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
		sb.append(msg.getMessage("AttributeStatements.copySubgroupAttribute")).append(" ");
		sb.append(condAttrStr);
		sb.append(" ").append(msg.getMessage("AttributeStatements.fromGroup")).append(" ");
		sb.append(a.getGroupPath());
		return sb.toString();
	}

	@Override
	public String getSupportedAttributeStatementName()
	{
		return CopySubgroupAttributeStatement.NAME;
	}
	
	public class CopyParentAttributeStatementComponent extends AbstractAttributeStatementComponent
	{
		private GroupComboBox conditionGroup;
		
		public CopyParentAttributeStatementComponent(UnityMessageSource msg,
				AttributeHandlerRegistry attrHandlerRegistry,
				Collection<AttributeType> attributeTypes, String group)
		{
			super(msg, attrHandlerRegistry, attributeTypes, group,
					new CopySubgroupAttributeStatement().getDescription());
			addConditionAttributeField(false);
			conditionGroup = new GroupComboBox(msg.getMessage("AttributeStatementEditDialog.inGroup"), 
					groupsMan);
			conditionGroup.setInput(group, false, false);
			main.addComponent(conditionGroup);
		}

		@Override
		public AttributeStatement getStatementFromComponent()
				throws FormValidationException
		{
			CopySubgroupAttributeStatement ret = new CopySubgroupAttributeStatement();
			Attribute<?> condAttr = getConditionAttribute();
			condAttr.setGroupPath((String) conditionGroup.getValue());
			ret.setConditionAttribute(condAttr);
			return ret;
		}

		@Override
		public void setInitialData(AttributeStatement initial)
		{
			Attribute<?> condAttr = initial.getConditionAttribute();
			if (condAttr != null)
			{
				conditionGroup.setValue(condAttr.getGroupPath());
				setConditionAttribute(condAttr);
			}
		}
	}
}
