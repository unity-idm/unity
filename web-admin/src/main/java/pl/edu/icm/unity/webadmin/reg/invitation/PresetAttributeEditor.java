/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.invitation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.FixedAttributeEditor;
import pl.edu.icm.unity.webui.common.composite.CompositeLayoutAdapter;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext.ConfirmationMode;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Editor of a prefilled invitation {@link Attribute}.
 * @author Krzysztof Benedyczak
 */
public class PresetAttributeEditor extends PresetEditorBase<Attribute>
{
	private List<AttributeRegistrationParam> formParams;
	private AttributeHandlerRegistry attrHandlersRegistry;
	private Map<String, AttributeType> attrTypes;
	
	private FixedAttributeEditor fixedAttributeEditor;
	private FormLayout wrapper;
	private AttributeRegistrationParam selectedParam;
	
	public PresetAttributeEditor(UnityMessageSource msg,
			List<AttributeRegistrationParam> formParams,
			AttributeHandlerRegistry attrHandlersRegistry,
			Map<String, AttributeType> attrTypes)
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
			throw new FormValidationException(msg.getMessage("PresetEditor.invalidEntry", 
					selectedParam.getAttributeType()), e);
		}
	}

	@Override
	public void setEditedComponentPosition(int position)
	{
		wrapper.removeAllComponents();
		selectedParam = formParams.get(position);
		AttributeType at = attrTypes.get(selectedParam.getAttributeType());
		
		AttributeEditContext editContext = AttributeEditContext.builder()
				.withConfirmationMode(ConfirmationMode.ADMIN).required()
				.withAttributeType(at)
				.withAttributeGroup(selectedParam.isUsingDynamicGroup() ? "/" : selectedParam.getGroup())
				.build();
		
		fixedAttributeEditor = new FixedAttributeEditor(msg, attrHandlersRegistry, 
			editContext, true, selectedParam.getAttributeType(), null);
		new CompositeLayoutAdapter(wrapper, fixedAttributeEditor.getComponentsGroup());
	}
	
	@Override
	protected Component getEditorComponentsInternal(
			PrefilledEntry<Attribute> value, int position)
	{
		wrapper = new FormLayout();
		setEditedComponentPosition(position);
		return wrapper;
	}
}
