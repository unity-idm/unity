/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.invitations.editor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.formlayout.FormLayout;

import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeEditContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.ConfirmationEditMode;
import io.imunity.vaadin.endpoint.common.plugins.attributes.FixedAttributeEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.LabelContext;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Editor of a prefilled invitation {@link Attribute}.
 * 
 * @author Krzysztof Benedyczak
 */
public class PresetAttributeEditor extends PresetEditorBase<Attribute>
{
	private final List<AttributeRegistrationParam> formParams;
	private final AttributeHandlerRegistry attrHandlersRegistry;
	private final Map<String, AttributeType> attrTypes;

	private FixedAttributeEditor fixedAttributeEditor;
	private FormLayout wrapper;
	private AttributeRegistrationParam selectedParam;

	public PresetAttributeEditor(MessageSource msg, List<AttributeRegistrationParam> formParams,
			AttributeHandlerRegistry attrHandlersRegistry, Map<String, AttributeType> attrTypes)
	{
		super(msg);
		this.formParams = formParams;
		this.attrHandlersRegistry = attrHandlersRegistry;
		this.attrTypes = attrTypes;
	}

	@Override
	protected Optional<Attribute> getValueInternal() throws FormValidationException
	{
		try
		{
			return fixedAttributeEditor.getAttribute();
		} catch (FormValidationException e)
		{
			throw new FormValidationException(
					msg.getMessage("PresetEditor.invalidEntry", selectedParam.getAttributeType()), e);
		}
	}

	@Override
	public void setEditedComponentPosition(int position)
	{
		wrapper.removeAll();
		selectedParam = formParams.get(position);
		AttributeType at = attrTypes.get(selectedParam.getAttributeType());

		AttributeEditContext editContext = AttributeEditContext.builder()
				.withConfirmationMode(ConfirmationEditMode.ADMIN)
				.required()
				.withAttributeType(at)
				.withCustomWidth(20)
				.withCustomWidthUnit(Unit.EM)
				.withAttributeGroup(selectedParam.isUsingDynamicGroup() ? "/" : selectedParam.getGroup())
				.build();

		fixedAttributeEditor = new FixedAttributeEditor(msg, attrHandlersRegistry, editContext,
				new LabelContext(selectedParam.getAttributeType()), null);
		for (Component component : fixedAttributeEditor.getComponentsGroup()
				.getComponents())
		{
			if (component instanceof HasLabel label)
			{
				wrapper.addFormItem(component, label.getLabel());
				label.setLabel("");
			} else
				wrapper.add(component);
		}
		super.setEditedComponentPosition(position);

	}

	@Override
	protected Component getEditorComponentsInternal(PrefilledEntry<Attribute> value, int position)
	{
		wrapper = new FormLayout();
		wrapper.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		setEditedComponentPosition(position);
		return wrapper;
	}

	@Override
	protected String getTitle()
	{
		return msg.getMessage("PresetEditor.activeAttribute", selectedParam.getAttributeType());
	}
}
