/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.facilities;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditor;
import io.imunity.vaadin.auth.authenticators.AuthenticatorEditorFactoriesRegistry;
import io.imunity.vaadin.auth.authenticators.SubViewSwitcher;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import io.imunity.vaadin.elements.NotificationPresenter;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.webElements.UnitySubView;

import java.util.*;
import java.util.stream.Collectors;

import static io.imunity.console.views.EditViewActionLayoutFactory.createActionLayout;

@PermitAll
@Route(value = "/facilities/authenticator", layout = ConsoleMenu.class)
public class AuthenticatorEditView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final AuthenticatorsController authenticatorsController;
	private final AuthenticatorEditorFactoriesRegistry editorsRegistry;
	private final NotificationPresenter notificationPresenter;

	private Collection<AuthenticatorTypeDescription> autnTypes;
	private AuthenticatorEditor editor;
	private boolean edit;
	private BreadCrumbParameter breadCrumbParameter;

	private VerticalLayout layout;
	private ComboBox<AuthenticatorTypeDescription> authenticatorTypeCombo;
	private Component editorComponent;

	AuthenticatorEditView(MessageSource msg, AuthenticatorsController authenticatorsController,
			AuthenticatorEditorFactoriesRegistry editorsRegistry, NotificationPresenter notificationPresenter,
			AuthenticatorManagement authnMan)
	{
		this.msg = msg;
		this.authenticatorsController = authenticatorsController;
		this.editorsRegistry = editorsRegistry;
		this.notificationPresenter = notificationPresenter;
		this.autnTypes = authnMan.getAvailableAuthenticatorsTypes();
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String id)
	{
		getContent().removeAll();

		AuthenticatorEntry entry;
		if(id == null)
		{
			entry = new AuthenticatorEntry(new AuthenticatorDefinition("", "", "", ""), List.of());
			breadCrumbParameter = new BreadCrumbParameter(null, msg.getMessage("new"));
			edit = false;
		}
		else
		{
			entry = authenticatorsController.getAuthenticator(id);
			breadCrumbParameter = new BreadCrumbParameter(id, id);
			edit = true;
		}
		initUI(entry);
	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	private void initUI(AuthenticatorEntry entry)
	{
		Map<AuthenticatorTypeDescription, String> authnTypesSorted = getAuthenticatorTypes();

		authenticatorTypeCombo = new ComboBox<>();
		authenticatorTypeCombo.addValueChangeListener(e -> reloadEditor(entry));

		authenticatorTypeCombo.setItemLabelGenerator(authnTypesSorted::get);
		authenticatorTypeCombo.setWidth(25, Unit.EM);
		authenticatorTypeCombo.setItems(authnTypesSorted.keySet());

		TextField authenticatorTypeLabel = new TextField();
		authenticatorTypeLabel.setWidth(25, Unit.EM);
		authenticatorTypeLabel.setReadOnly(true);

		FormLayout typeWrapper = new FormLayout();
		typeWrapper.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		typeWrapper.addFormItem(authenticatorTypeCombo, msg.getMessage("MainAuthenticatorEditor.typeComboCaption"));
		typeWrapper.addFormItem(authenticatorTypeLabel, msg.getMessage("MainAuthenticatorEditor.typeLabelCaption"));
		layout = new VerticalLayout(typeWrapper);
		layout.setPadding(false);

		if (edit)
		{
			AuthenticatorTypeDescription desc = authnTypesSorted.keySet().stream()
					.filter(t -> t.getVerificationMethod().equals(entry.authenticator().type))
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
		getContent().add(new VerticalLayout(layout, createActionLayout(msg, edit, FacilitiesView.class, this::onConfirm)));
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

	private void reloadEditor(AuthenticatorEntry entry)
	{

		AuthenticatorTypeDescription type = authenticatorTypeCombo.getValue();
		if (editorComponent != null)
		{
			layout.remove(editorComponent);
		}

		try
		{
			editor = editorsRegistry.getByName(type.getVerificationMethod()).createInstance();
			editorComponent = editor.getEditor(entry.authenticator(),
					new SubViewSwitcher()
					{
						@Override
						public void exitSubView()
						{

						}

						@Override
						public void goToSubView(UnitySubView subview)
						{

						}

						@Override
						public void exitSubViewAndShowUpdateInfo()
						{

						}
					}, !edit);
			layout.add(editorComponent);
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("MainAuthenticatorEditor.createSingleAuthenticatorEditorError"), e.getMessage());
		}
	}

	private void onConfirm()
	{
		AuthenticatorDefinition authenticatorDefinition;
		try
		{
			authenticatorDefinition = editor.getAuthenticatorDefinition();
		} catch (FormValidationException e)
		{
			notificationPresenter.showError(msg.getMessage("EditAuthenticatorView.invalidConfiguration"),
					e.getMessage());
			return;
		}

		if(edit)
			authenticatorsController.updateAuthenticator(authenticatorDefinition);
		else
			authenticatorsController.addAuthenticator(authenticatorDefinition);
		UI.getCurrent().navigate(FacilitiesView.class);
	}
}
