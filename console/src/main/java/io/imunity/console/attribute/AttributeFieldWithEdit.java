/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.attribute;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.Collection;

import static io.imunity.vaadin.elements.CSSVars.BASE_MARGIN;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.VaadinClassNames.FIELD_ICON_GAP;
import static io.imunity.vaadin.elements.VaadinClassNames.POINTER;

public class AttributeFieldWithEdit extends CustomField<Attribute>
{
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final AttributeHandlerRegistry attrHandlerRegistry;
	private final Collection<AttributeType> attributeTypes;
	protected String group;
	private final TextField attributeTF;
	private final boolean valuesRequired;
	
	private Attribute attribute;
	private AttributeType fixedAttributeType;
	
	public AttributeFieldWithEdit(MessageSource msg, String caption, 
			AttributeHandlerRegistry attrHandlerRegistry,
			Collection<AttributeType> attributeTypes, String group, Attribute initial,
			boolean valuesRequired, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.group = group;
		this.valuesRequired = valuesRequired;
		this.attributeTypes = attributeTypes;
		setLabel(caption);
		this.attribute = initial;
		attributeTF = new TextField();
		attributeTF.setValue(msg.getMessage("AttributeField.noAttribute"));
		attributeTF.setEnabled(false);
		attributeTF.addValueChangeListener(this::fireEvent);
		attributeTF.setWidth(TEXT_FIELD_MEDIUM.value());
		Icon icon = VaadinIcon.EDIT.create();
		icon.setTooltipText(msg.getMessage("AttributeField.edit"));
		icon.addClassNames(POINTER.getName(), FIELD_ICON_GAP.getName());
		icon.getStyle().set("margin-top", BASE_MARGIN.value());
		icon.addClickListener(e -> editAttribute());
		HorizontalLayout hl = new HorizontalLayout();
		hl.setSpacing(false);
		hl.add(attributeTF, icon);
		add(hl);
	}

	private void editAttribute()
	{
		AttributeEditor theEditor = fixedAttributeType == null ? 
				new AttributeEditor(msg, attributeTypes, null, group, attrHandlerRegistry, valuesRequired) :
				new AttributeEditor(msg, fixedAttributeType, null, group, attrHandlerRegistry);
		if (attribute != null)
			theEditor.setInitialAttribute(attribute);
		AttributeEditDialog dialog = new AttributeEditDialog(msg, 
				msg.getMessage("AttributeField.edit"), newAttribute ->
		{
			setAttribute(newAttribute);
			return true;
		}, theEditor, notificationPresenter);
		dialog.open();
	}
	
	private void checkAttributeSet() throws FormValidationException
	{
		if (attribute == null)
		{
			attributeTF.setErrorMessage(
					msg.getMessage("AttributeField.attributeRequired"));
			attributeTF.setInvalid(true);
			throw new FormValidationException();
		}
		attributeTF.setInvalid(false);
		attributeTF.setErrorMessage(null);
	}
	
	public Attribute getAttribute() throws FormValidationException
	{
		checkAttributeSet();
		return attribute;
	}
	

	public void setAttribute(Attribute attribute)
	{
		this.attribute = attribute;
		String attrRep = attrHandlerRegistry.getSimplifiedAttributeRepresentation(attribute);
		attributeTF.setEnabled(true);
		attributeTF.setValue(attrRep);
		attributeTF.setEnabled(false);
		attributeTF.setErrorMessage(null);
	}

	public void setFixedType(AttributeType attributeType)
	{
		this.fixedAttributeType = attributeType;
	}

	@Override
	public Attribute getValue() 
	{
		try
		{
			return getAttribute();
		} catch (FormValidationException e)
		{
			return null;
		}
	}

	@Override
	protected Attribute generateModelValue()
	{
		return getValue();
	}

	@Override
	protected void setPresentationValue(Attribute attribute)
	{
		setAttribute(attribute);
	}
}
