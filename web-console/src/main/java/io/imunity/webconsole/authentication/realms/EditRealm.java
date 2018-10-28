/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

import io.imunity.webconsole.common.AbstractConfirmView;
import io.imunity.webconsole.common.ControllerException;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * View for edit realm
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class EditRealm extends AbstractConfirmView
{

	private RealmController controller;
	private AuthenticationRealmEditor editor;

	@Autowired
	public EditRealm(UnityMessageSource msg, RealmController controller)
	{
		super(msg, msg.getMessage("ok", msg.getMessage("cancel")));
		this.controller = controller;
	}

	@Override
	protected void onConfirm()
	{
		if (editor.hasErrors())
		{
			return;
		}

		try
		{
			if (!controller.updateRealm(editor.getAuthenticationRealm()))
				return;
		} catch (ControllerException e)
		{

			NotificationPopup.showError(e.getErrorCaption(), e.getErrorDetails());
			return;
		}

		UI.getCurrent().getNavigator().navigateTo(Realms.class.getSimpleName());

	}

	@Override
	protected void onCancel()
	{
		UI.getCurrent().getNavigator().navigateTo(Realms.class.getSimpleName());

	}

	@Override
	protected Component getContents(Map<String, String> parameters) throws Exception
	{
	
		String realmName = parameters.isEmpty() ? "" :  parameters.keySet().iterator().next();

		AuthenticationRealm realm;
		try
		{
			realm = controller.getRealm(realmName);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e.getErrorCaption(), e.getErrorDetails());
			UI.getCurrent().getNavigator().navigateTo(Realms.class.getSimpleName());
			return null;
		}

		editor = new AuthenticationRealmEditor(msg, realm);
		editor.editMode();

		return editor;
	}
}
