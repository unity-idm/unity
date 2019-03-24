/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.authenticators;

import java.util.Collection;

import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactoriesRegistry;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * 
 * @author P.Piernik
 *
 */
public class MainAuthenticatorEditor extends CustomComponent
{
	private ComboBox<AuthenticatorTypeDescription> authenticatorType;
	private UnityMessageSource msg;
	private AuthenticatorEditorFactoriesRegistry editorsRegistry;
	private Collection<AuthenticatorTypeDescription> autnTypes;
	private AuthenticatorEntry toEdit;
	private SubViewSwitcher subViewSwitcher;

	private AuthenticatorEditor editor;
	private Component editorComponent;
	private VerticalLayout mainLayout;

	public static final String STATE_KEY = "MainAuthenticatorEditorState";

	public MainAuthenticatorEditor(UnityMessageSource msg, AuthenticatorEditorFactoriesRegistry editorsRegistry,
			Collection<AuthenticatorTypeDescription> autnTypes, AuthenticatorEntry toEdit,
			SubViewSwitcher subViewSwitcher)
	{
		this.msg = msg;
		this.toEdit = toEdit;
		this.editorsRegistry = editorsRegistry;
		this.autnTypes = autnTypes;
		this.subViewSwitcher = subViewSwitcher;
		initUI();
	}

	private void initUI()
	{
		authenticatorType = new ComboBox<AuthenticatorTypeDescription>();
		authenticatorType.setCaption(msg.getMessage("AuthenticatorEditor.typeCaption"));
		authenticatorType.addSelectionListener(e -> reloadEditor());
		authenticatorType.setEmptySelectionAllowed(false);
		authenticatorType.setItemCaptionGenerator(
				t -> t.getVerificationMethod() + " (" + t.getVerificationMethodDescription() + ")");
		authenticatorType.setWidth(50, Unit.EM);
		authenticatorType.setItems(autnTypes);

		mainLayout = new VerticalLayout();
		mainLayout.setMargin(false);
		FormLayoutWithFixedCaptionWidth typeWrapper = new FormLayoutWithFixedCaptionWidth();
		typeWrapper.setMargin(new MarginInfo(false, true));
		typeWrapper.addComponent(authenticatorType);
		mainLayout.addComponent(typeWrapper);

		setCompositionRoot(mainLayout);

		if (toEdit != null)
		{
			authenticatorType.setValue(autnTypes.stream()
					.filter(t -> t.getVerificationMethod().equals(toEdit.authneticator.type))
					.findFirst().orElse(null));
			authenticatorType.setReadOnly(true);
		} else
		{
			Object state = VaadinSession.getCurrent().getAttribute(STATE_KEY);

			if (state == null)
			{

				authenticatorType.setValue(autnTypes.iterator().next());
			} else
			{
				AuthenticatorTypeDescription type = (AuthenticatorTypeDescription) state;
				authenticatorType.setValue(autnTypes.stream().filter(
						t -> t.getVerificationMethod().equals(type.getVerificationMethod()))
						.findAny().get());
			}
		}
	}

	private void reloadEditor()
	{

		AuthenticatorTypeDescription type = authenticatorType.getValue();
		VaadinSession.getCurrent().setAttribute(STATE_KEY, type);
		if (editorComponent != null)
		{
			mainLayout.removeComponent(editorComponent);
		}

		try
		{
			editor = editorsRegistry.getByName(type.getVerificationMethod()).createInstance();
			editorComponent = editor.getEditor(toEdit != null ? toEdit.authneticator : null,
					subViewSwitcher, false);
			mainLayout.addComponent(editorComponent);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("MainAuthenticatorEditor.getSingleAuthenticatorEditorError"), e);
		}
	}

	AuthenticatorDefinition getAuthenticator() throws FormValidationException
	{
		if (editor == null)
			throw new FormValidationException();

		return editor.getAuthenticatorDefiniton();
	}

}
