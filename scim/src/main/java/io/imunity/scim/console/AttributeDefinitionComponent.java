/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import io.imunity.scim.schema.SCIMAttributeType;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;

class AttributeDefinitionComponent extends CustomField<AttributeDefinitionBean>
{
	private final MessageSource msg;
	private TextField name;
	private Binder<AttributeDefinitionBean> binder;
	private final VerticalLayout headerLayout;
	private final VerticalLayout subAttrLayout;
	private final boolean disableMultiAndComplex;
	private final boolean readOnly;

	AttributeDefinitionComponent(MessageSource msg, boolean readOnly, boolean disableMultiAndComplex,
			VerticalLayout attrDefHeaderSlot, VerticalLayout subAttrSlot)
	{
		this.msg = msg;
		this.headerLayout = attrDefHeaderSlot;
		this.subAttrLayout = subAttrSlot;
		this.disableMultiAndComplex = disableMultiAndComplex;
		this.readOnly = readOnly;
		init();
	}

	private void init()
	{
		binder = new Binder<>(AttributeDefinitionBean.class);
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(false);
		headerLayout.addComponent(header);

		name = new TextField(msg.getMessage("AttributeDefinitionConfigurationEditor.name"));
		header.addComponent(name);
		name.setReadOnly(readOnly);
		binder.forField(name).asRequired().bind("name");

		TextField desc = new TextField(msg.getMessage("AttributeDefinitionConfigurationEditor.description"));
		header.addComponent(desc);
		desc.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
		desc.setReadOnly(readOnly);
		binder.forField(desc).bind("description");

		ComboBox<SCIMAttributeType> type = new ComboBox<>();
		type.setCaption(msg.getMessage("AttributeDefinitionConfigurationEditor.type"));

		type.setItems(Stream.of(SCIMAttributeType.values())
				.filter(t -> !disableMultiAndComplex || !t.equals(SCIMAttributeType.COMPLEX))
				.collect(Collectors.toList()));
		type.setItemCaptionGenerator(t -> t.getName());
		type.setEmptySelectionAllowed(false);
		type.setValue(SCIMAttributeType.STRING);
		type.setReadOnly(readOnly);
		header.addComponent(type);
		binder.forField(type).bind("type");

		CheckBox multi = new CheckBox();
		multi.setCaption(msg.getMessage("AttributeDefinitionConfigurationEditor.multiValued"));
		header.addComponent(multi);
		binder.forField(multi).bind("multiValued");
		multi.setVisible(!disableMultiAndComplex);
		multi.setReadOnly(readOnly);

		FormLayoutWithFixedCaptionWidth subAttrFormLayout = new FormLayoutWithFixedCaptionWidth();
		subAttrFormLayout.setMargin(false);
		subAttrLayout.addComponent(subAttrFormLayout);
		AttributeDefinitionConfigurationList attributesList = new AttributeDefinitionConfigurationList(msg,
				msg.getMessage("AttributeDefinitionConfigurationList.addSubAttribute"), true, readOnly);
		attributesList.setRequiredIndicatorVisible(false);
		binder.forField(attributesList)
				.withValidator(
						(value, context) -> (value == null || value.stream().filter(a -> a == null).count() > 0)
								? ValidationResult.error(msg.getMessage("fieldRequired"))
								: ValidationResult.ok())
				.bind("subAttributesWithMapping");
		type.addValueChangeListener(v ->
		{
			if (!v.getValue().equals(SCIMAttributeType.COMPLEX))
			{
				attributesList.clear();
			}
			attributesList.setVisible(v.getValue().equals(SCIMAttributeType.COMPLEX));
		});
		attributesList.setVisible(false);
		subAttrFormLayout.addComponent(attributesList);

		binder.addValueChangeListener(
				e -> fireEvent(new ValueChangeEvent<>(this, binder.getBean(), e.isUserOriginated())));
		binder.setValidatorsDisabled(true);
		headerLayout.addAttachListener(e -> binder.setValidatorsDisabled(false));
	}

	@Override
	public AttributeDefinitionBean getValue()
	{
		if (binder.validate().hasErrors())
		{
			return null;
		}
		return binder.getBean();
	}

	@Override
	protected Component initContent()
	{
		return new VerticalLayout(headerLayout, subAttrLayout);
	}

	@Override
	protected void doSetValue(AttributeDefinitionBean value)
	{
		binder.setBean(value);
	}

	String getHeaderText()
	{
		return name.getValue() == null || name.getValue().isEmpty() ? "" : name.getValue();

	}
}