/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.authenticators;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
		Map<AuthenticatorTypeDescription, String> authnTypesSorted = getAuthenticatorTypes();

		authenticatorType = new ComboBox<AuthenticatorTypeDescription>();
		authenticatorType.setCaption(msg.getMessage("AuthenticatorEditor.typeCaption"));
		authenticatorType.addSelectionListener(e -> reloadEditor());
		authenticatorType.setEmptySelectionAllowed(false);
		authenticatorType.setItemCaptionGenerator(t -> authnTypesSorted.get(t));
		authenticatorType.setWidth(25, Unit.EM);
		authenticatorType.setItems(authnTypesSorted.keySet());

		mainLayout = new VerticalLayout();
		mainLayout.setMargin(false);
		FormLayoutWithFixedCaptionWidth typeWrapper = new FormLayoutWithFixedCaptionWidth();
		typeWrapper.setMargin(new MarginInfo(false, true));
		typeWrapper.addComponent(authenticatorType);
		mainLayout.addComponent(typeWrapper);

		setCompositionRoot(mainLayout);

		if (toEdit != null)
		{
			authenticatorType.setValue(authnTypesSorted.keySet().stream()
					.filter(t -> t.getVerificationMethod().equals(toEdit.authenticator.type))
					.findFirst().orElse(null));
			authenticatorType.setReadOnly(true);
		} else
		{

			authenticatorType.setValue(authnTypesSorted.keySet().iterator().next());

		}
	}

	private String getAuthenticatorTypeLabel(AuthenticatorTypeDescription t)
	{
		try
		{
			return msg.getMessageUnsafe("Verificator." + t.getVerificationMethod());
		} catch (Exception e)
		{
			return t.getVerificationMethod() + " (" + t.getVerificationMethodDescription() + ")";
		}
	}

	private Map<AuthenticatorTypeDescription, String> getAuthenticatorTypes()
	{
		Map<AuthenticatorTypeDescription, String> res = new LinkedHashMap<>();

		for (AuthenticatorTypeDescription type : autnTypes)
		{
			res.put(type, getAuthenticatorTypeLabel(type));
		}

		return res.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors
				.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
	}

	private void reloadEditor()
	{

		AuthenticatorTypeDescription type = authenticatorType.getValue();
		if (editorComponent != null)
		{
			mainLayout.removeComponent(editorComponent);
		}

		try
		{
			editor = editorsRegistry.getByName(type.getVerificationMethod()).createInstance();
			editorComponent = editor.getEditor(toEdit != null ? toEdit.authenticator : null,
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
