/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.authenticators;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.event.selection.SingleSelectionListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorTypeDescription;
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
	private ComboBox<AuthenticatorTypeDescription> authenticatorTypeCombo;
	private TextField authenticatorTypeLabel;
	private MessageSource msg;
	private AuthenticatorEditorFactoriesRegistry editorsRegistry;
	private Collection<AuthenticatorTypeDescription> autnTypes;
	private AuthenticatorEntry toEdit;
	private SubViewSwitcher subViewSwitcher;

	private AuthenticatorEditor editor;
	private Component editorComponent;
	private VerticalLayout mainLayout;
	private Optional<SingleSelectionListener<AuthenticatorTypeDescription>> typeChangeListener;

	public MainAuthenticatorEditor(MessageSource msg, AuthenticatorEditorFactoriesRegistry editorsRegistry,
			Collection<AuthenticatorTypeDescription> autnTypes, AuthenticatorEntry toEdit,
			SubViewSwitcher subViewSwitcher, Optional<SingleSelectionListener<AuthenticatorTypeDescription>> typeChangeListener)
	{
		this.msg = msg;
		this.toEdit = toEdit;
		this.editorsRegistry = editorsRegistry;
		this.autnTypes = autnTypes;
		this.subViewSwitcher = subViewSwitcher;
		this.typeChangeListener = typeChangeListener;
		initUI();
	}

	private void initUI()
	{
		Map<AuthenticatorTypeDescription, String> authnTypesSorted = getAuthenticatorTypes();

		authenticatorTypeCombo = new ComboBox<>();
		authenticatorTypeCombo.setCaption(msg.getMessage("MainAuthenticatorEditor.typeComboCaption"));
		authenticatorTypeCombo.addSelectionListener(e -> reloadEditor());
		if (typeChangeListener.isPresent())
		{
			authenticatorTypeCombo.addSelectionListener(typeChangeListener.get());
		}
		authenticatorTypeCombo.setEmptySelectionAllowed(false);
		authenticatorTypeCombo.setItemCaptionGenerator(t -> authnTypesSorted.get(t));
		authenticatorTypeCombo.setWidth(25, Unit.EM);
		authenticatorTypeCombo.setItems(authnTypesSorted.keySet());

		authenticatorTypeLabel = new TextField();
		authenticatorTypeLabel.setWidth(25, Unit.EM);
		authenticatorTypeLabel.setCaption(msg.getMessage("MainAuthenticatorEditor.typeLabelCaption"));
		authenticatorTypeLabel.setReadOnly(true);
		
		mainLayout = new VerticalLayout();
		mainLayout.setMargin(false);
		FormLayoutWithFixedCaptionWidth typeWrapper = new FormLayoutWithFixedCaptionWidth();
		typeWrapper.setMargin(new MarginInfo(false, true));
		typeWrapper.addComponent(authenticatorTypeCombo);
		typeWrapper.addComponent(authenticatorTypeLabel);
		mainLayout.addComponent(typeWrapper);

		setCompositionRoot(mainLayout);

		if (toEdit != null)
		{
			AuthenticatorTypeDescription desc = authnTypesSorted.keySet().stream()
					.filter(t -> t.getVerificationMethod().equals(toEdit.authenticator.type))
					.findFirst().orElse(null);
			
			authenticatorTypeCombo.setValue(desc);
			authenticatorTypeCombo.setVisible(false);
			authenticatorTypeLabel.setValue(desc != null ? AuthenticatorTypeLabelHelper.getAuthenticatorTypeLabel(msg, desc) : "");
			authenticatorTypeLabel.setVisible(true);
		} else
		{
			authenticatorTypeCombo.setVisible(true);
			authenticatorTypeLabel.setVisible(false);
			authenticatorTypeCombo.setValue(authnTypesSorted.keySet().iterator().next());

		}
	}

	private Map<AuthenticatorTypeDescription, String> getAuthenticatorTypes()
	{
		Map<AuthenticatorTypeDescription, String> res = new LinkedHashMap<>();

		for (AuthenticatorTypeDescription type : autnTypes)
		{
			res.put(type, AuthenticatorTypeLabelHelper.getAuthenticatorTypeLabel(msg, type));
		}

		return res.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors
				.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
	}

	private void reloadEditor()
	{

		AuthenticatorTypeDescription type = authenticatorTypeCombo.getValue();
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
					msg.getMessage("MainAuthenticatorEditor.createSingleAuthenticatorEditorError"), e);
		}
	}
	
	public void addTypeChangeListener(SingleSelectionListener<AuthenticatorTypeDescription> listener)
	{
		authenticatorTypeCombo.addSelectionListener(listener);
	}
	
	AuthenticatorDefinition getAuthenticator() throws FormValidationException
	{
		if (editor == null)
			throw new FormValidationException();

		return editor.getAuthenticatorDefiniton();
	}

}
