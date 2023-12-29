/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.invitations.editor;

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;

import io.imunity.vaadin.elements.CSSVars;
import io.imunity.vaadin.elements.EnumComboBox;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.ListOfEmbeddedElementsStub.Editor;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntryMode;
import pl.edu.icm.unity.webui.common.FormValidationException;


/**
 * Base class for editors of prefilled entries
 * 
 * @author Krzysztof Benedyczak
 */
public abstract class PresetEditorBase <T> implements Editor<PrefilledEntry<T>>
{
	private ComponentsContainer container;
	protected MessageSource msg;
	
	private Checkbox active;
	private EnumComboBox<PrefilledEntryMode> mode;
	
	public PresetEditorBase(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public ComponentsContainer getEditorComponent(PrefilledEntry<T> value, int position)
	{
		container = new ComponentsContainer();
		active = new Checkbox();
		active.addValueChangeListener(event -> {
			setEnabled(active.getValue());
		});
		mode = new EnumComboBox<>(msg::getMessage, "PrefilledEntryMode.", 
				PrefilledEntryMode.class, PrefilledEntryMode.DEFAULT);		
		mode.setWidth(CSSVars.TEXT_FIELD_MEDIUM.value());
		FormLayout wrapper = new FormLayout();
		wrapper.addFormItem(mode, msg.getMessage("PresetEditor.mode"));
		
		
		container.add(active, wrapper);
		
		Component editorUI = getEditorComponentsInternal(value, position);
		container.add(editorUI);
		setEnabled(active.getValue());
		active.setLabel(getTitle());
		return container;
	}
	
	@Override
	public void setEditedComponentPosition(int position)
	{
		active.setLabel(getTitle());
	}

	private void setEnabled(boolean enabled)
	{
		for (int i=1; i<container.getComponents().length; i++)
		{
			Component component = container.getComponents()[i];
			if (component instanceof HasEnabled ec)
			{
				ec.setEnabled(enabled);
			}
		}
	}

	/**
	 * @return the edited value
	 * @throws FormValidationException 
	 */
	protected abstract Optional<T> getValueInternal() throws FormValidationException;
	
	/**
	 * @return the components of the editor
	 */
	protected abstract Component getEditorComponentsInternal(PrefilledEntry<T> value, int position);
	
	protected abstract String getTitle();
	
	@Override
	public PrefilledEntry<T> getValue() throws FormValidationException
	{
		if (active.getValue())
		{
			Optional<T> value = getValueInternal();
			return value.isPresent() ? new PrefilledEntry<>(value.get(), mode.getValue()) : null;	
		}
		return null;
	}
}
