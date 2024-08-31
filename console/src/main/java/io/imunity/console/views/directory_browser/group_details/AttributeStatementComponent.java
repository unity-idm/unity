/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.group_details;

import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;

import io.imunity.console.attribute.AttributeFieldWithEdit;
import io.imunity.console.tprofile.AttributeSelectionComboBox;
import io.imunity.vaadin.endpoint.common.TooltipFactory;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import io.imunity.vaadin.endpoint.common.mvel.MVELExpressionField;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeStatementMVELContextKey;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;;


class AttributeStatementComponent extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AttributeStatementComponent.class);
	private static final String MODE_FIXED = "fixed";
	private static final String MODE_DYNAMIC = "dynamic";
	
	private final MessageSource msg;
	private final Set<String> groups;
	private final Collection<AttributeType> attributeTypes;
	private final AttributeHandlerRegistry attrHandlerRegistry;
	private final String group;
	
	private Checkbox extraAttributesGroupCB;
	private GroupComboBox extraAttributesGroupCombo;
	private RadioButtonGroup<String> assignMode;

	private Binder<AttributeStatement> binder;
	
	
	AttributeStatementComponent(MessageSource msg, GroupsManagement groupsMan,
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

	private Set<String> getGroupsOfHierarchy(GroupsManagement groupsMan,
			String hierarchyElement)
	{
		try
		{
			Set<String> allGroups = groupsMan.getChildGroups("/");
			return allGroups.stream().filter(g -> !g.equals(hierarchyElement)
					&& (g.startsWith(hierarchyElement)
							|| hierarchyElement.startsWith(g)))
					.collect(Collectors.toSet());
		} catch (EngineException e)
		{
			log.warn("Can not read child groups", e);
			return new HashSet<>();
		}
	}

	private void initUI()
	{
		extraAttributesGroupCB = new Checkbox(
				msg.getMessage("AttributeStatementComponent.extraGroupCB"));
		extraAttributesGroupCB.addValueChangeListener((e) -> {
			extraAttributesGroupCombo.setEnabled(extraAttributesGroupCB.getValue());
			extraAttributesGroupCombo.setRequiredIndicatorVisible(extraAttributesGroupCB.getValue());
			if (extraAttributesGroupCB.getValue() && extraAttributesGroupCombo.getValue() == null && !groups.isEmpty())
				extraAttributesGroupCombo.setValue(extraAttributesGroupCombo.getAllGroups().get(0));
		});
		extraAttributesGroupCB.setTooltipText(
				msg.getMessage("AttributeStatementComponent.extraGroupCBDesc"));

		extraAttributesGroupCombo = new GroupComboBox("", groups);
		extraAttributesGroupCombo.setEnabled(false);
		extraAttributesGroupCombo.setInput(group, false);
		extraAttributesGroupCombo.setRequiredIndicatorVisible(false);
		
		if (groups.isEmpty())
			extraAttributesGroupCB.setEnabled(false);

		TooltipFactory tooltipFactory = new TooltipFactory();
		MVELExpressionField condition = new MVELExpressionField(msg, "",
				msg.getMessage("MVELExpressionField.conditionDesc"),
				MVELExpressionContext.builder()
						.withTitleKey("AttributeStatementComponent.conditionTitle")
						.withEvalToKey("MVELExpressionField.evalToBoolean")
						.withVars(AttributeStatementMVELContextKey.toMap()).build(), tooltipFactory);
		condition.setValue("true");

		assignMode = new RadioButtonGroup<>();
		Map<String, String> captions = ImmutableMap.of(MODE_DYNAMIC,
				msg.getMessage("AttributeStatementComponent.dynamicMode"),
				MODE_FIXED,
				msg.getMessage("AttributeStatementComponent.fixedMode"));
		assignMode.setItems(captions.keySet());
		assignMode.setItemLabelGenerator(captions::get);
		assignMode.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

		ComboBox<AttributeType> dynamicAttributeName = new AttributeSelectionComboBox("", attributeTypes, msg);
		MVELExpressionField dynamicAttributeValue = new MVELExpressionField(msg,
				"",
				msg.getMessage("AttributeStatementComponent.dynamicAttrValueDesc"),
				MVELExpressionContext.builder()
						.withTitleKey("AttributeStatementComponent.dynamicAttrValueTitle")
						.withEvalToKey("AttributeStatementComponent.evalToListOfAttributeValues")
						.withVars(AttributeStatementMVELContextKey.toMap()).build(), tooltipFactory);

		AttributeFieldWithEdit fixedAttribute = new AttributeFieldWithEdit(msg, "",
				attrHandlerRegistry, attributeTypes, group, null, true);

		ComboBox<ConflictResolution> conflictResolution = new ComboBox<>();
		conflictResolution.setItems(ConflictResolution.values());
		conflictResolution.setItemLabelGenerator(item -> msg.getMessage(
				"AttributeStatement.conflictResolution." + item.toString()));
		conflictResolution.setValue(ConflictResolution.skip);

		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		main.addFormItem(extraAttributesGroupCB, "");
		main.addFormItem(extraAttributesGroupCombo, msg.getMessage("AttributeStatementComponent.extraGroupSelect"));
		main.addFormItem(condition, msg.getMessage("AttributeStatementComponent.condition"));
		main.addFormItem(assignMode, "");
		FormLayout.FormItem dynamicAttrNameItem = main.addFormItem(dynamicAttributeName,
				msg.getMessage("AttributeStatementComponent.dynamicAttrName"));
		FormLayout.FormItem dynamicAttrValueItem = main.addFormItem(dynamicAttributeValue,
				msg.getMessage("AttributeStatementComponent.dynamicAttrValue"));
		FormLayout.FormItem fixedAttributeFormItem = main.addFormItem(fixedAttribute,
				msg.getMessage("AttributeStatementComponent.fixedAttr"));
		main.addFormItem(conflictResolution, msg.getMessage("AttributeStatementEditDialog.conflictResolution"));
		add(main);

		assignMode.addValueChangeListener((e) -> {
			boolean fixed = assignMode.getValue().equals(MODE_FIXED);
			fixedAttributeFormItem.setVisible(fixed);
			dynamicAttrNameItem.setVisible(!fixed);
			dynamicAttrValueItem.setVisible(!fixed);
		});
		assignMode.setValue(MODE_DYNAMIC);


		binder = new Binder<>(AttributeStatement.class);

		binder.forField(extraAttributesGroupCombo)
				.asRequired(msg.getMessage("fieldRequired"))
				.bind("extraAttributesGroup");
		condition.configureBinding(binder, "condition", true);
		binder.forField(fixedAttribute).asRequired(msg.getMessage("fieldRequired"))
				.bind("fixedAttribute");
		Map<String, AttributeType> typesMap = attributeTypes.stream()
				.collect(Collectors.toMap(AttributeType::getName, t -> t));
		binder.forField(dynamicAttributeName)
				.withConverter(d -> d != null ? d.getName() : "", typesMap::get, "FOO")
				.asRequired((Validator<String>) (value, context) ->
				{
					if (StringUtils.isBlank(value) && assignMode.getValue().equals(MODE_DYNAMIC))
						return ValidationResult.error(msg.getMessage("fieldRequired"));
					else
						return ValidationResult.ok();
				})
				.bind("dynamicAttributeType");
		dynamicAttributeValue.configureBinding(binder,"dynamicAttributeExpression", true);	
		binder.forField(conflictResolution).asRequired(msg.getMessage("fieldRequired"))
				.bind("conflictResolution");

		AttributeStatement attrStatment = new AttributeStatement();
		attrStatment.setCondition(condition.getValue());
		attrStatment.setConflictResolution(ConflictResolution.skip);
		attrStatment.setExtraAttributesGroup(extraAttributesGroupCombo.getValue());
		attrStatment.setDynamicAttributeType(dynamicAttributeName.getValue().getName());
		binder.setBean(attrStatment);
	}

	void setInitialData(AttributeStatement initial)
	{
		binder.setBean(initial);
		extraAttributesGroupCB.setValue(initial.getExtraAttributesGroup() != null);
		assignMode.setValue(initial.dynamicAttributeMode() ? MODE_DYNAMIC : MODE_FIXED);
	}

	private void validateBinding(String name) throws FormValidationException
	{
		if (binder.getBinding(name).get().validate().isError())
			throw new FormValidationException();
	}
	
	AttributeStatement getStatementFromComponent() throws FormValidationException
	{
		AttributeStatement ret = binder.getBean();

		validateBinding("condition");
		validateBinding("conflictResolution");
		
		if (!extraAttributesGroupCB.getValue())
		{
			ret.setExtraAttributesGroup(null);
		} else
		{
			validateBinding("extraAttributesGroup");
			//'/' is mapped to null what we don't want
			ret.setExtraAttributesGroup(extraAttributesGroupCombo.getValue());
		}

		if (assignMode.getValue().equals(MODE_DYNAMIC))
		{
			validateBinding("dynamicAttributeType");
			validateBinding("dynamicAttributeExpression");	
			ret.setFixedAttribute(null);
		} else
		{
			validateBinding("fixedAttribute");
			ret.setDynamicAttributeExpression(null);
			ret.setDynamicAttributeType(null);
		}

		return ret;
	}
}
