/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.groupdetails;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.attributes.AttributeValueChecker;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatementCondition;
import pl.edu.icm.unity.types.basic.AttributeStatementCondition.Type;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.webadmin.attribute.AttributeEditDialog;
import pl.edu.icm.unity.webadmin.attribute.AttributeEditor;
import pl.edu.icm.unity.webadmin.attribute.AttributeSelectionComboBox;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupComboBox;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.FormLayoutDoubleComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Allows to create/edit/view an attribute statement of a group.
 * @author K. Benedyczak
 */
public class AttributeStatementEditDialog extends AbstractDialog
{
	private AttributeStatement statement;
	private GroupsManagement groupsMan;
	private AttributesManagement attrsMan;
	private AttributeHandlerRegistry attrHandlerRegistry;
	private final String group;
	private final Callback callback;
	
	private TextField assignedAttributeTF;
	private Attribute<?> assignedAttribute;
	private EnumComboBox<AttributeStatementCondition.Type> condition;

	private Panel conditionArgs;
	private Map<Type, ConditionPanel> conditionPanelsMap;
	private Collection<AttributeType> attributeTypes;

	private EnumComboBox<AttributeStatement.ConflictResolution> conflictResolution;
	
	/**
	 * @param msg
	 * @param attributeStatement attribute statement to be displayed initially or 
	 * null when a new statement should be created
	 */
	public AttributeStatementEditDialog(UnityMessageSource msg, String group, AttributeStatement attributeStatement,
			GroupsManagement groupsMan, AttributeHandlerRegistry attrHandlerRegistry,
			AttributesManagement attrsMan, Callback callback)
	{
		super(msg, msg.getMessage("AttributeStatementEditDialog.caption"));
		this.statement = attributeStatement;
		this.groupsMan = groupsMan;
		this.attrsMan = attrsMan;
		this.defaultSizeUndfined = true;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.group = group;
		this.callback = callback;
	}

	@Override
	protected Component getContents() throws Exception
	{
		try
		{
			attributeTypes = attrsMan.getAttributeTypes();
		} catch(Exception e)
		{
			ErrorPopup.showError(msg.getMessage("AttributeStatementEditDialog.cantReadAttributeTypes"), e);
			throw e;
		}
		
		assignedAttributeTF = new TextField(msg.getMessage("AttributeStatementEditDialog.assignedAttribute"));
		assignedAttributeTF.setValue(msg.getMessage("AttributeStatementEditDialog.noAttribute"));
		assignedAttributeTF.setReadOnly(true);
		assignedAttributeTF.setWidth(100, Unit.PERCENTAGE);
		Button editAssignedAttribute = new Button();
		editAssignedAttribute.setIcon(Images.edit.getResource());
		editAssignedAttribute.setDescription(msg.getMessage("AttributeStatementEditDialog.edit"));
		editAssignedAttribute.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				editAssignedAttribute();
			}
		});
		FormLayoutDoubleComponent assignedAttributePanel = new FormLayoutDoubleComponent(assignedAttributeTF, 
				editAssignedAttribute);
		assignedAttributePanel.setWidth(100, Unit.PERCENTAGE);
		
		condition = new EnumComboBox<Type>(msg.getMessage("AttributeStatementEditDialog.condition"), 
				msg, "AttributeStatements.condType.", 
				Type.class, Type.everybody);
		condition.setImmediate(true);
		condition.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				conditionChanged();
			}
		});
		
		conditionArgs = new Panel();
		
		conflictResolution = new EnumComboBox<AttributeStatement.ConflictResolution>(
				msg.getMessage("AttributeStatementEditDialog.conflictResolution"), msg, 
				"AttributeStatement.conflictResolution.", AttributeStatement.ConflictResolution.class, 
				AttributeStatement.ConflictResolution.skip);
				
		FormLayout ret = new FormLayout(assignedAttributePanel, condition, conditionArgs, conflictResolution);
		
		initConditionGUI();
		if (statement != null)
			setInitialData(statement);
		return ret;
	}

	private void initConditionGUI()
	{
		conditionPanelsMap = new HashMap<AttributeStatementCondition.Type, ConditionPanel>();
		conditionPanelsMap.put(Type.everybody, new EverybodyCondPanel());
		conditionPanelsMap.put(Type.memberOf, new MemberOfCondPanel());
		conditionPanelsMap.put(Type.hasParentgroupAttribute, new ParentAttrCondPanel());
		conditionPanelsMap.put(Type.hasParentgroupAttributeValue, new ParentAttrWithValueCondPanel());
		conditionPanelsMap.put(Type.hasSubgroupAttribute, new SubgroupAttrCondPanel());
		conditionPanelsMap.put(Type.hasSubgroupAttributeValue, new SubgroupAttrWithValueCondPanel());
	}
	
	private ConditionPanel getCurrentConditionComponent()
	{
		Type type = condition.getSelectedValue();
		return conditionPanelsMap.get(type);
	}
	
	private void setInitialData(AttributeStatement attributeStatement)
	{
		setAttributeField(assignedAttributeTF, attributeStatement.getAssignedAttribute());
		assignedAttribute = attributeStatement.getAssignedAttribute();
		AttributeStatementCondition condition = attributeStatement.getCondition();
		this.condition.setEnumValue(condition.getType());
		getCurrentConditionComponent().setCondition(condition);
		conflictResolution.setEnumValue(attributeStatement.getConflictResolution());
	}
	
	private void setAttributeField(TextField tf, Attribute<?> assignedAttribute)
	{
		String attrRep = attrHandlerRegistry.getSimplifiedAttributeRepresentation(assignedAttribute);
		tf.setReadOnly(false);
		tf.setValue(attrRep);
		tf.setReadOnly(true);
		tf.setComponentError(null);
	}
	
	private void conditionChanged()
	{
		conditionArgs.setContent(getCurrentConditionComponent().getComponent());
	}
	
	private void editAssignedAttribute()
	{
		editAttribute(assignedAttribute, group, new AttributeEditDialog.Callback()
		{
			@Override
			public boolean newAttribute(Attribute<?> newAttribute)
			{
				setAttributeField(assignedAttributeTF, newAttribute);
				assignedAttribute = newAttribute;
				return true;
			}
		});
	}

	private void editAttribute(Attribute<?> initial, String groupPath, AttributeEditDialog.Callback callback)
	{
		AttributeEditor theEditor = new AttributeEditor(msg, attributeTypes, groupPath, attrHandlerRegistry);
		if (initial != null)
			theEditor.setInitialAttribute(initial);
		AttributeEditDialog dialog = new AttributeEditDialog(msg, 
				msg.getMessage("AttributeStatementEditDialog.attributeEdit"), callback, theEditor);
		dialog.show();
	}
	
	@Override
	protected void onConfirm()
	{
		AttributeStatement ret = new AttributeStatement();
		if (assignedAttribute == null)
		{
			assignedAttributeTF.setComponentError(new UserError(msg.getMessage(
					"AttributeStatementEditDialog.attributeRequired")));
			return;
		}
		
		for (AttributeType type: attributeTypes)
		{
			if (type.getName().equals(assignedAttribute.getName()))
			{
				try
				{
					AttributeValueChecker.validate(assignedAttribute, type);
				} catch (Exception e)
				{
					assignedAttributeTF.setComponentError(new UserError(msg.getMessage(
							"AttributeStatementEditDialog.attributeInvalid", e.getMessage())));
					return;
				}
			}
		}
		
		
		assignedAttributeTF.setComponentError(null);
		
		ret.setAssignedAttribute(assignedAttribute);
		try
		{
			ret.setCondition(getCurrentConditionComponent().getCondition());
		} catch (FormValidationException e)
		{
			return;
		}
		ret.setConflictResolution(conflictResolution.getSelectedValue());
		callback.onConfirm(ret);
		close();
	}

	
	private class EverybodyCondPanel implements ConditionPanel
	{
		private VerticalLayout dummy = new VerticalLayout();
		
		@Override
		public AttributeStatementCondition getCondition()
		{
			return new AttributeStatementCondition(Type.everybody);
		}

		@Override
		public void setCondition(AttributeStatementCondition condition)
		{
		}

		@Override
		public Component getComponent()
		{
			return dummy;
		}
	}

	private class MemberOfCondPanel implements ConditionPanel
	{
		private GroupComboBox conditionAnyGroup;
		private FormLayout main;
		
		public MemberOfCondPanel()
		{
			conditionAnyGroup = new GroupComboBox(msg.getMessage("AttributeStatementEditDialog.group"), 
					groupsMan);
			conditionAnyGroup.setInput("/", true);
			main = new FormLayout(conditionAnyGroup);
			main.setMargin(true);
		}
		
		@Override
		public AttributeStatementCondition getCondition() throws FormValidationException
		{
			AttributeStatementCondition ret = new AttributeStatementCondition(Type.memberOf);
			String group = (String)conditionAnyGroup.getValue();
			if (group == null)
			{
				conditionAnyGroup.setComponentError(new UserError(msg.getMessage(
						"AttributeStatementEditDialog.groupRequired")));
				throw new FormValidationException();
			}
			conditionAnyGroup.setComponentError(null);
			ret.setGroup(group);
			return ret;
		}

		@Override
		public void setCondition(AttributeStatementCondition condition)
		{
			String group = condition.getGroup();
			conditionAnyGroup.select(group);
		}

		@Override
		public Component getComponent()
		{
			return main;
		}
	}

	private class ParentAttrCondPanel implements ConditionPanel
	{
		protected AttributeSelectionComboBox condAttrNoValue;
		protected FormLayout main;
		
		public ParentAttrCondPanel()
		{
			try
			{
				condAttrNoValue = new AttributeSelectionComboBox(
						msg.getMessage("AttributeStatementEditDialog.condAttr"), attrsMan);
			} catch (EngineException e)
			{
				// ignored... authz error - nothing will be possible. Anyway shouldn't be displayed.
			}
			main = new FormLayout(condAttrNoValue);
			main.setMargin(true);
		}

		@Override
		public AttributeStatementCondition getCondition() throws FormValidationException
		{
			AttributeStatementCondition ret = new AttributeStatementCondition(Type.hasParentgroupAttribute);
			AttributeType attribute = condAttrNoValue.getSelectedValue();
			if (attribute == null)
			{
				condAttrNoValue.setComponentError(new UserError(msg.getMessage(
						"AttributeStatementEditDialog.attributeRequired")));
				throw new FormValidationException();
			}
			condAttrNoValue.setComponentError(null);
			
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Attribute a = new Attribute(attribute.getName(), attribute.getValueType(), group, 
					AttributeVisibility.full, null);
			ret.setAttribute(a);
			return ret;
		}

		@Override
		public void setCondition(AttributeStatementCondition condition)
		{
			condAttrNoValue.select(condition.getAttribute().getName());
		}

		@Override
		public Component getComponent()
		{
			return main;
		}
	}

	private class ParentAttrWithValueCondPanel implements ConditionPanel
	{
		protected TextField conditionAttributeTF;
		protected Attribute<?> condAttr;
		protected FormLayout main;
		
		public ParentAttrWithValueCondPanel()
		{
			conditionAttributeTF = new TextField(msg.getMessage("AttributeStatementEditDialog.condAttr"));
			conditionAttributeTF.setValue(msg.getMessage("AttributeStatementEditDialog.noAttribute"));
			conditionAttributeTF.setReadOnly(true);
			conditionAttributeTF.setWidth(100, Unit.PERCENTAGE);
			Button condAttrEditButton = new Button();
			condAttrEditButton.setIcon(Images.edit.getResource());
			condAttrEditButton.setDescription(msg.getMessage("AttributeStatementEditDialog.edit"));
			condAttrEditButton.addClickListener(new ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					editAttribute(condAttr, "", new AttributeEditDialog.Callback()
					{
						@Override
						public boolean newAttribute(Attribute<?> newAttribute)
						{
							setAttributeField(conditionAttributeTF, newAttribute);
							condAttr = newAttribute;
							return true;
						}
					});
				}
			});
			FormLayoutDoubleComponent condAttrWithValuePanel = new FormLayoutDoubleComponent(
					conditionAttributeTF, condAttrEditButton);
			condAttrWithValuePanel.setWidth(100, Unit.PERCENTAGE);
			main = new FormLayout(condAttrWithValuePanel);
			main.setMargin(true);
		}

		@Override
		public AttributeStatementCondition getCondition() throws FormValidationException
		{
			if (condAttr == null)
			{
				conditionAttributeTF.setComponentError(new UserError(msg.getMessage(
						"AttributeStatementEditDialog.attributeRequired")));
				throw new FormValidationException();
			}
			
			if (condAttr.getValues().isEmpty())
			{
				conditionAttributeTF.setComponentError(new UserError(msg.getMessage(
						"AttributeStatementEditDialog.attributeValuesRequired")));
				throw new FormValidationException();
			}
				
			
			AttributeStatementCondition ret = new AttributeStatementCondition(
					Type.hasParentgroupAttributeValue);
			ret.setAttribute(condAttr);
			return ret;
		}

		@Override
		public void setCondition(AttributeStatementCondition condition)
		{
			setConditionAttribute(condition.getAttribute());
		}

		private void setConditionAttribute(Attribute<?> attribute)
		{
			setAttributeField(conditionAttributeTF, attribute);
			condAttr = attribute;
		}

		@Override
		public Component getComponent()
		{
			return main;
		}
	}

	
	private class SubgroupAttrCondPanel extends ParentAttrCondPanel
	{
		private GroupComboBox conditionSubGroup;
		
		public SubgroupAttrCondPanel()
		{
			super();
			conditionSubGroup = new GroupComboBox(msg.getMessage("AttributeStatementEditDialog.group"), 
					groupsMan);
			conditionSubGroup.setInput(group, false);
			main.addComponent(conditionSubGroup);
		}

		@Override
		public AttributeStatementCondition getCondition() throws FormValidationException
		{
			AttributeStatementCondition ret = super.getCondition();
			ret.setType(Type.hasSubgroupAttribute);
			String selectedGroup = (String) conditionSubGroup.getValue();
			if (selectedGroup == null)
			{
				conditionSubGroup.setComponentError(new UserError(msg.getMessage(
						"AttributeStatementEditDialog.groupRequired")));
				throw new FormValidationException();
			}
			conditionSubGroup.setComponentError(null);
			ret.getAttribute().setGroupPath(selectedGroup);
			return ret;
		}

		@Override
		public void setCondition(AttributeStatementCondition condition)
		{
			super.setCondition(condition);
			conditionSubGroup.select(condition.getAttribute().getGroupPath());
		}
	}

	private class SubgroupAttrWithValueCondPanel extends ParentAttrWithValueCondPanel
	{
		private GroupComboBox conditionSubGroup;
		
		public SubgroupAttrWithValueCondPanel()
		{
			super();
			conditionSubGroup = new GroupComboBox(msg.getMessage("AttributeStatementEditDialog.group"), 
					groupsMan);
			conditionSubGroup.setInput(group, false);
			main.addComponent(conditionSubGroup);
		}

		@Override
		public AttributeStatementCondition getCondition() throws FormValidationException
		{
			AttributeStatementCondition ret = super.getCondition();
			ret.setType(Type.hasSubgroupAttributeValue);
			String selectedGroup = (String) conditionSubGroup.getValue();
			if (selectedGroup == null)
			{
				conditionSubGroup.setComponentError(new UserError(msg.getMessage(
						"AttributeStatementEditDialog.groupRequired")));
				throw new FormValidationException();
			}
			conditionSubGroup.setComponentError(null);
			ret.getAttribute().setGroupPath(selectedGroup);
			return ret;
		}

		@Override
		public void setCondition(AttributeStatementCondition condition)
		{
			super.setCondition(condition);
			conditionSubGroup.select(condition.getAttribute().getGroupPath());
		}
	}
	
	public interface Callback
	{
		public void onConfirm(AttributeStatement newStatement);
	}

}
