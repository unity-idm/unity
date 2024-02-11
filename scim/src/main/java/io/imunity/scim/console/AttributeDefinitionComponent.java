/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;

import io.imunity.scim.console.mapping.AttributeDefinitionBean;
import io.imunity.scim.schema.SCIMAttributeType;
import io.imunity.vaadin.elements.CSSVars;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import pl.edu.icm.unity.base.message.MessageSource;

class AttributeDefinitionComponent extends CustomField<AttributeDefinitionBean>
{
	private final MessageSource msg;
	private final HtmlTooltipFactory htmlTooltipFactory;
	private final VerticalLayout headerLayout;
	private final VerticalLayout subAttrLayout;
	private final AttributeEditContext context;
	private final AttributeEditorData attributeEditorData;
	private AttributeEditContext editContext;
	private TextField name;
	private Binder<AttributeDefinitionBean> binder;
	
	AttributeDefinitionComponent(MessageSource msg, HtmlTooltipFactory htmlTooltipFactory, AttributeEditContext context,
			AttributeEditorData attributeEditorData, VerticalLayout attrDefHeaderSlot, VerticalLayout subAttrSlot)
	{
		this.msg = msg;
		this.htmlTooltipFactory = htmlTooltipFactory;
		this.headerLayout = attrDefHeaderSlot;
		this.subAttrLayout = subAttrSlot;
		this.context = context;
		this.attributeEditorData = attributeEditorData;
		init();
	}

	private void init()
	{
		binder = new Binder<>(AttributeDefinitionBean.class);

		FormLayout header = new FormLayout();
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		headerLayout.add(header);

		name = new TextField();
		header.addFormItem(name, msg.getMessage("AttributeDefinitionConfigurationEditor.name"));
		name.setReadOnly(!context.attributesEditMode.equals(AttributesEditMode.FULL_EDIT));
		name.setWidth(CSSVars.TEXT_FIELD_BIG.value());
		binder.forField(name)
				.asRequired()
				.bind("name");

		TextField desc = new TextField();
		header.addFormItem(desc, msg.getMessage("AttributeDefinitionConfigurationEditor.description"));
		desc.setWidth(CSSVars.TEXT_FIELD_BIG.value());
		desc.setReadOnly(!context.attributesEditMode.equals(AttributesEditMode.FULL_EDIT));
		binder.forField(desc)
				.bind("description");

		ComboBox<SCIMAttributeType> type = new ComboBox<>();
		type.setItems(Stream.of(SCIMAttributeType.values())
				.filter(t -> !context.disableComplexAndMulti || !t.equals(SCIMAttributeType.COMPLEX))
				.collect(Collectors.toList()));
		type.setItemLabelGenerator(t -> t.getName());
		type.setValue(SCIMAttributeType.STRING);
		type.setReadOnly(!context.attributesEditMode.equals(AttributesEditMode.FULL_EDIT));
		header.addFormItem(type, msg.getMessage("AttributeDefinitionConfigurationEditor.type"));
		binder.forField(type)
				.bind("type");

		Checkbox multi = new Checkbox();
		multi.setLabel(msg.getMessage("AttributeDefinitionConfigurationEditor.multiValued"));
		header.addFormItem(multi, "");
		binder.forField(multi)
				.bind("multiValued");
		multi.setVisible(!context.disableComplexAndMulti);
		multi.setReadOnly(!context.attributesEditMode.equals(AttributesEditMode.FULL_EDIT));

		FormLayout subAttrFormLayout = new FormLayout();
		subAttrFormLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		subAttrFormLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		subAttrLayout.add(subAttrFormLayout);
		editContext = AttributeEditContext.builder()
				.withDisableComplexAndMulti(true)
				.withAttributesEditMode(context.attributesEditMode)
				.build();
		AttributeDefinitionConfigurationList attributesList = new AttributeDefinitionConfigurationList(msg,
				htmlTooltipFactory, msg.getMessage("AttributeDefinitionConfigurationList.addSubAttribute"),
				() -> editContext, attributeEditorData);
		attributesList.setRequiredIndicatorVisible(false);

		binder.forField(attributesList)
				.withValidator((value, context) -> (value == null || value.stream()
						.filter(a -> a == null)
						.count() > 0) ? ValidationResult.error(msg.getMessage("fieldRequired")) : ValidationResult.ok())
				.bind("subAttributesWithMapping");
		multi.addValueChangeListener(e ->
		{
			editContext = AttributeEditContext.builder()
					.withDisableComplexAndMulti(true)
					.withAttributesEditMode(context.attributesEditMode)
					.withComplexMultiParent(e.getValue())
					.build();
			attributesList.refreshEditors();
		});

		type.addValueChangeListener(v ->
		{
			if (!v.getValue()
					.equals(SCIMAttributeType.COMPLEX))
			{
				attributesList.clear();
			}
			attributesList.setVisible(v.getValue()
					.equals(SCIMAttributeType.COMPLEX));
		});
		attributesList.setVisible(false);
		subAttrFormLayout.add(attributesList);

		binder.addValueChangeListener(
				e -> fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), e.isFromClient())));

		binder.setValidatorsDisabled(true);
		add(new VerticalLayout(headerLayout, subAttrLayout));
	}

	String getHeaderText()
	{
		return name.getValue() == null || name.getValue()
				.isEmpty() ? "" : name.getValue();

	}
	
	void setValidatorsDisabled(boolean validadors)
	{
		binder.setValidatorsDisabled(validadors);
	}

	@Override
	public AttributeDefinitionBean getValue()
	{
		if (binder.validate()
				.hasErrors())
		{
			return null;
		}
		return binder.getBean();
	}

	@Override
	protected AttributeDefinitionBean generateModelValue()
	{
		return getValue();
	}

	@Override
	protected void setPresentationValue(AttributeDefinitionBean newPresentationValue)
	{
		binder.setBean(newPresentationValue);

	}
}