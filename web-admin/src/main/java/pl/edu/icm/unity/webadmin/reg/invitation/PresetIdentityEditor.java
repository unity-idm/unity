/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.webadmin.reg.invitation;

import java.util.List;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;

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
			List<IdentityRegistrationParam> formParameters, UnityMessageSource msg)
	{
		super(msg);
		this.identityEditorRegistry = identityEditorRegistry;
		this.formParameters = formParameters;
	}

	@Override
	protected IdentityParam getValueInternal() throws FormValidationException
	{
		try
		{
			return editor.getValue();
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
		container.addComponents(editor.getEditor(true, false).getComponents());
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
