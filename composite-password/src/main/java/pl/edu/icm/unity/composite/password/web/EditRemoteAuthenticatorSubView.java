/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.composite.password.web;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.composite.password.CompositePasswordProperties.VerificatorTypes;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.pam.PAMVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.common.webElements.UnitySubView;

/**
 * SubView for editing remote authenticator
 * 
 * @author P.Piernik
 *
 */
class EditRemoteAuthenticatorSubView extends CustomComponent implements UnitySubView
{
	private MessageSource msg;
	private AuthenticatorEditorFactory factory;
	private AuthenticatorDefinition toEdit;

	private boolean editMode;

	EditRemoteAuthenticatorSubView(MessageSource msg, AuthenticatorEditorFactory factory,
			AuthenticatorDefinition toEdit, Consumer<AuthenticatorDefinition> onConfirm, Runnable onCancel,
			SubViewSwitcher subViewSwitcher)
	{

		this.msg = msg;
		this.factory = factory;
		this.toEdit = toEdit;

		editMode = toEdit != null;

		AuthenticatorEditor editor;
		try
		{
			editor = factory.createInstance();
		} catch (EngineException e)
		{
			throw new InternalException("Can not create remote authenticator editor");
		}

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);

		Runnable onConfirmR = () -> {
			try
			{
				onConfirm.accept(editor.getAuthenticatorDefiniton());
			} catch (FormValidationException e)
			{
				NotificationPopup.showError(msg,
						msg.getMessage("EditRemoteAuthenticatorSubView.invalidConfiguration"),
						e);
			}
		};

		mainView.addComponent(editor.getEditor(toEdit, subViewSwitcher, true));

		mainView.addComponent(toEdit != null
				? StandardButtonsHelper.buildConfirmEditButtonsBar(msg, onConfirmR, onCancel)
				: StandardButtonsHelper.buildConfirmNewButtonsBar(msg, onConfirmR, onCancel));

		setCompositionRoot(mainView);

	}

	@Override
	public List<String> getBredcrumbs()
	{
		List<String> breadcrumbs = new ArrayList<>();
		breadcrumbs.add(msg.getMessage("EditRemoteAuthenticatorSubView.caption"));
		breadcrumbs.add(factory.getSupportedAuthenticatorType().equals(PAMVerificator.NAME)
				? VerificatorTypes.pam.toString()
				: VerificatorTypes.ldap.toString());
		if (editMode)
		{
			breadcrumbs.add(toEdit.id);
		}

		return breadcrumbs;
	}
}
