/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.invitations.editor;

import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.formlayout.FormLayout;

import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditor;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorContext;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.webui.common.FormValidationException;


/**
 * Editor of a prefilled invitation {@link IdentityParam}.
 * 
 * @author Krzysztof Benedyczak
 */
public class PresetIdentityEditor extends PresetEditorBase<IdentityParam>
{
	private IdentityEditorRegistry identityEditorRegistry;
	private List<IdentityRegistrationParam> formParameters;
	private IdentityRegistrationParam selectedParam;
	
	private FormLayout container;
	private IdentityEditor editor;
	
	public PresetIdentityEditor(IdentityEditorRegistry identityEditorRegistry,
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
		container.removeAll();
		ComponentsContainer editorComponent = editor.getEditor(IdentityEditorContext.builder()
					.withRequired(true)
					.withCustomWidth(20)
					.withCustomWidthUnit(Unit.EM)
					.withAdminMode(true).build());
		
		for (Component component : editorComponent.getComponents())
		{
			if (component instanceof HasLabel label)
			{
				container.addFormItem(component,  label.getLabel());
				label.setLabel(null);
			}
			else
			container.addFormItem(component, "");
		}
	}

	@Override
	protected Component getEditorComponentsInternal(
			PrefilledEntry<IdentityParam> value, int position)
	{
		container = new FormLayout();
		container.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		setEditedComponentPosition(position);
		return container;
	}
}
