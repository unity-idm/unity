/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.composite.password.web;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactory;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.api.UnitySubView;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.composite.password.CompositePasswordProperties;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.pam.PAMVerificator;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static io.imunity.vaadin.elements.CssClassNames.EDIT_VIEW_ACTION_BUTTONS_LAYOUT;


class EditRemoteAuthenticatorSubView extends VerticalLayout implements UnitySubView
{
	private final MessageSource msg;
	private final AuthenticatorEditorFactory factory;
	private final AuthenticatorDefinition toEdit;

	private final boolean editMode;

	EditRemoteAuthenticatorSubView(MessageSource msg, AuthenticatorEditorFactory factory,
			AuthenticatorDefinition toEdit, Consumer<AuthenticatorDefinition> onConfirm, Runnable onCancel,
			SubViewSwitcher subViewSwitcher, NotificationPresenter notificationPresenter)
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

		mainView.add(editor.getEditor(toEdit, subViewSwitcher, true));

		Button cancelButton = new Button(msg.getMessage("cancel"), event -> onCancel.run());
		cancelButton.setWidthFull();
		Button updateButton = new Button(editMode ? msg.getMessage("update") :  msg.getMessage("create"), event ->
		{
			try
			{
				onConfirm.accept(editor.getAuthenticatorDefinition());
			} catch (FormValidationException e)
			{
				notificationPresenter.showError(
						msg.getMessage("EditRemoteAuthenticatorSubView.invalid"), e.getMessage());
			}
		});
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		updateButton.setWidthFull();
		HorizontalLayout buttonsLayout = new HorizontalLayout(cancelButton, updateButton);
		buttonsLayout.setClassName(EDIT_VIEW_ACTION_BUTTONS_LAYOUT.getName());
		mainView.add(buttonsLayout);

		add(mainView);
	}

	@Override
	public List<String> getBreadcrumbs()
	{
		List<String> breadcrumbs = new ArrayList<>();
		breadcrumbs.add(msg.getMessage("EditRemoteAuthenticatorSubView.caption"));
		breadcrumbs.add(factory.getSupportedAuthenticatorType().equals(PAMVerificator.NAME)
				? CompositePasswordProperties.VerificatorTypes.pam.toString()
				: CompositePasswordProperties.VerificatorTypes.ldap.toString());
		if (editMode)
		{
			breadcrumbs.add(toEdit.id);
		}

		return breadcrumbs;
	}
}
