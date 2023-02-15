/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.invitations.editor;

import java.util.List;
import java.util.Optional;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorContext;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistryV8;

/**
 * Editor of a prefilled invitation {@link IdentityParam}.
 * 
 * @author Krzysztof Benedyczak
 */
public class PresetIdentityEditor extends PresetEditorBase<IdentityParam>
{
	private IdentityEditorRegistryV8 identityEditorRegistry;
	private List<IdentityRegistrationParam> formParameters;
	private IdentityRegistrationParam selectedParam;
	
	private FormLayout container;
	private IdentityEditor editor;
	
	public PresetIdentityEditor(IdentityEditorRegistryV8 identityEditorRegistry,
	                            List<IdentityRegistrationParam> formParameters, MessageSource msg)
	{
		super(msg);
		this.identityEditorRegistry = identityEditorRegistry;
		this.formParameters = formParameters;
	}

	@Override
	protected Optional<IdentityParam> getValueInternal() throws FormValidationException
	{
		try
		{
			return Optional.ofNullable(editor.getValue());
		} catch (IllegalIdentityValueException e)
		{
			throw new FormValidationException(msg.getMessage("PresetEditor.invalidEntry", 
					selectedParam.getIdentityType()), e);
		}
	}

	@Override
	public void setEditedComponentPosition(int position)
	{
		selectedParam = formParameters.get(position);
		editor = identityEditorRegistry.getEditor(selectedParam.getIdentityType());
		container.removeAllComponents();
		ComponentsContainer editorComponent = editor.getEditor(IdentityEditorContext.builder()
					.withRequired(true)
					.withAdminMode(true).build());
		container.addComponents(editorComponent.getComponents());
	}

	@Override
	protected Component getEditorComponentsInternal(
			PrefilledEntry<IdentityParam> value, int position)
	{
		container = new FormLayout();
		setEditedComponentPosition(position);
		return container;
	}
}
