/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.invitation;

import java.util.Optional;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.Editor;

/**
 * Base class for editors of prefilled entries
 * 
 * @author Krzysztof Benedyczak
 */
public abstract class PresetEditorBase <T> implements Editor<PrefilledEntry<T>>
{
	private ComponentsContainer container;
	protected UnityMessageSource msg;
	
	private CheckBox active;
	private EnumComboBox<PrefilledEntryMode> mode;
	
	public PresetEditorBase(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public ComponentsContainer getEditorComponent(PrefilledEntry<T> value, int position)
	{
		container = new ComponentsContainer();
		active = new CheckBox(msg.getMessage("PresetEditor.active"));
		active.addValueChangeListener(event -> {
			setEnabled(active.getValue());
		});
		mode = new EnumComboBox<>(msg, "PrefilledEntryMode.", 
				PrefilledEntryMode.class, PrefilledEntryMode.DEFAULT);
		container.add(active, mode);
		mode.setWidth(20, Unit.EM);
		
		Component editorUI = getEditorComponentsInternal(value, position);
		container.add(editorUI);
		setEnabled(active.getValue());
		return container;
	}

	private void setEnabled(boolean enabled)
	{
		for (int i=1; i<container.getComponents().length; i++)
			container.getComponents()[i].setEnabled(enabled);
	}

	/**
	 * @return the edited value
	 */
	protected abstract Optional<T> getValueInternal() throws FormValidationException;
	
	/**
	 * @return the components of the editor
	 */
	protected abstract Component getEditorComponentsInternal(PrefilledEntry<T> value, int position);
	
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
