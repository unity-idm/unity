/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attrstmt;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeStatement2;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.webadmin.attribute.AttributeFieldWithEdit;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GroupComboBox;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.AttributeSelectionComboBox;

import com.vaadin.data.Validator;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;

/**
 * Editing of a single {@link AttributeStatement2}.
 * @author K. Benedyczak
 */
public class AttributeStatementComponent extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AttributeStatementComponent.class);
	private static final String MODE_FIXED = "fixed";
	private static final String MODE_DYNAMIC = "dynamic";
	
	private final UnityMessageSource msg;
	private final Set<String> groups;
	private final Collection<AttributeType> attributeTypes;
	private final AttributeHandlerRegistry attrHandlerRegistry;
	private final String group;
	
	private CheckBox extraAttributesGroupCB;
	private GroupComboBox extraAttributesGroupCombo;
	private TextField condition;
	
	private OptionGroup assignMode;
	
	private AttributeSelectionComboBox dynamicAttributeName;
	private EnumComboBox<AttributeVisibility> visibilityCombo;
	private TextField dynamicAttributeValue;
	
	private AttributeFieldWithEdit fixedAttribute;
	
	private EnumComboBox<AttributeStatement2.ConflictResolution> conflictResolution;
	
	
	public AttributeStatementComponent(UnityMessageSource msg, GroupsManagement groupsMan,
			Collection<AttributeType> attributeTypes,
			AttributeHandlerRegistry attrHandlerRegistry, String group)
	{
		this.msg = msg;
		this.groups = getGroupsOfHierarchy(groupsMan, group);
		this.attributeTypes = attributeTypes;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.group = group;
		initUI();
	}


	private Set<String> getGroupsOfHierarchy(GroupsManagement groupsMan, String hierarchyElement)
	{
		try
		{
			Set<String> allGroups = groupsMan.getChildGroups("/");
			return allGroups.stream().filter(g -> 
					!g.equals(hierarchyElement) && (g.startsWith(hierarchyElement) || 
					hierarchyElement.startsWith(g))).
				collect(Collectors.toSet());
		} catch (EngineException e)
		{
			log.warn("Can not read child groups", e);
			return new HashSet<>();
		}
	}
	
	private void initUI()
	{
		extraAttributesGroupCB = new CheckBox(msg.getMessage("AttributeStatementComponent.extraGroupCB"));
		extraAttributesGroupCB.addValueChangeListener((e) -> {
			extraAttributesGroupCombo.setEnabled(extraAttributesGroupCB.getValue());
		});
		extraAttributesGroupCB.setDescription(msg.getMessage("AttributeStatementComponent.extraGroupCBDesc"));
		extraAttributesGroupCombo = new GroupComboBox(
				msg.getMessage("AttributeStatementComponent.extraGroupSelect"), groups);
		extraAttributesGroupCombo.setEnabled(false);
		extraAttributesGroupCombo.setInput(group, false);
		
		if (groups.isEmpty())
			extraAttributesGroupCB.setEnabled(false);
		Validator expressionValidator = v -> {
			try
			{
				MVEL.compileExpression((String)v);
			} catch (Exception e)
			{
				throw new Validator.InvalidValueException(
						msg.getMessage("AttributeStatementComponent.invalidExpression", 
								e.getMessage()));
			}
		}; 
		
		condition = new TextField(msg.getMessage("AttributeStatementComponent.condition"));
		condition.setDescription(msg.getMessage("AttributeStatementComponent.conditionDesc"));
		condition.setWidth(100, Unit.PERCENTAGE);
		condition.addValidator(expressionValidator);
		condition.setRequiredError(msg.getMessage("fieldRequired"));
		condition.setRequired(true);
		
		assignMode = new OptionGroup();
		assignMode.addItem(MODE_DYNAMIC);
		assignMode.setItemCaption(MODE_DYNAMIC, msg.getMessage("AttributeStatementComponent.dynamicMode"));
		assignMode.addItem(MODE_FIXED);
		assignMode.setItemCaption(MODE_FIXED, msg.getMessage("AttributeStatementComponent.fixedMode"));
		assignMode.addValueChangeListener((e) -> {
			boolean fixed = assignMode.getValue().equals(MODE_FIXED);
			fixedAttribute.setVisible(fixed);
			dynamicAttributeName.setVisible(!fixed);
			visibilityCombo.setVisible(!fixed);
			dynamicAttributeValue.setVisible(!fixed);
		});
		
		dynamicAttributeName = new AttributeSelectionComboBox(
				msg.getMessage("AttributeStatementComponent.dynamicAttrName"), attributeTypes);
		visibilityCombo = new EnumComboBox<AttributeVisibility>(
				msg.getMessage("AttributeStatementComponent.dynamicAttrVisibility"), msg, 
				"AttributeVisibility.", AttributeVisibility.class, AttributeVisibility.full);
		dynamicAttributeValue = new TextField(
				msg.getMessage("AttributeStatementComponent.dynamicAttrValue"));
		dynamicAttributeValue.setDescription(
				msg.getMessage("AttributeStatementComponent.dynamicAttrValueDesc"));
		dynamicAttributeValue.setWidth(100, Unit.PERCENTAGE);
		dynamicAttributeValue.addValidator(expressionValidator);
		dynamicAttributeValue.setRequiredError(msg.getMessage("fieldRequired"));
		dynamicAttributeValue.setRequired(true);

		
		fixedAttribute = new AttributeFieldWithEdit(msg, 
				msg.getMessage("AttributeStatementComponent.fixedAttr"), 
				attrHandlerRegistry, attributeTypes, 
				group, null, true);
		
		assignMode.select(MODE_DYNAMIC);
		
		conflictResolution = new EnumComboBox<AttributeStatement2.ConflictResolution>(
				msg.getMessage("AttributeStatementEditDialog.conflictResolution"), msg, 
				"AttributeStatement.conflictResolution.", AttributeStatement2.ConflictResolution.class, 
				AttributeStatement2.ConflictResolution.skip);
		
		FormLayout main = new CompactFormLayout();
		setCompositionRoot(main);
		main.addComponents(extraAttributesGroupCB, extraAttributesGroupCombo, condition, assignMode,
				dynamicAttributeName, dynamicAttributeValue, visibilityCombo, fixedAttribute,
				conflictResolution);
	}
	
	
	public void setInitialData(AttributeStatement2 initial)
	{
		if (initial.getExtraAttributesGroup() != null)
		{
			extraAttributesGroupCB.setValue(true);
			extraAttributesGroupCombo.setValue(initial.getExtraAttributesGroup());
		} else
			extraAttributesGroupCB.setValue(false);
		
		condition.setValue(initial.getCondition());
		
		if (initial.dynamicAttributeMode())
		{
			assignMode.setValue(MODE_DYNAMIC);
			dynamicAttributeName.setValue(initial.getDynamicAttributeType());
			visibilityCombo.setValue(initial.getDynamicAttributeVisibility());
			dynamicAttributeValue.setValue(initial.getDynamicAttributeExpression());
		} else
		{
			assignMode.setValue(MODE_FIXED);
			fixedAttribute.setAttribute(initial.getFixedAttribute());
		}
		conflictResolution.setEnumValue(initial.getConflictResolution());
	}
	
	public AttributeStatement2 getStatementFromComponent() throws FormValidationException
	{
		if (!condition.isValid())
			throw new FormValidationException();
		
		AttributeStatement2 ret = new AttributeStatement2();
		
		if (extraAttributesGroupCB.getValue())
		{
			ret.setExtraAttributesGroup((String) extraAttributesGroupCombo.getValue());
		}
		
		ret.setCondition(condition.getValue());
		
		if (assignMode.getValue().equals(MODE_DYNAMIC))
		{
			if (!dynamicAttributeValue.isValid())
				throw new FormValidationException();
			ret.setDynamicAttributeVisibility(visibilityCombo.getSelectedValue());
			ret.setDynamicAttributeExpression(dynamicAttributeValue.getValue());
			ret.setDynamicAttributeType(dynamicAttributeName.getSelectedValue());
		} else
		{
			ret.setFixedAttribute(fixedAttribute.getAttribute());
		}
		ret.setConflictResolution(conflictResolution.getSelectedValue());
		return ret;
	}
}
