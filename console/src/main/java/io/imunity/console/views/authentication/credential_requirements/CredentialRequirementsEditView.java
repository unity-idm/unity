/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.credential_requirements;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import org.vaadin.tatu.TwinColSelect;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.message.MessageSource;

import javax.annotation.security.PermitAll;
import java.util.Collection;
import java.util.HashSet;

@PermitAll
@Breadcrumb(key = "edit")
@Route(value = "/credential-requirements/edit", layout = ConsoleMenu.class)
public class CredentialRequirementsEditView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final CredentialRequirementsController controller;
	private final EventsBus bus;
	private Binder<CredentialRequirements> binder;
	private boolean edit;

	CredentialRequirementsEditView(MessageSource msg, CredentialRequirementsController controller)
	{
		this.msg = msg;
		this.controller = controller;
		this.bus = WebSession.getCurrent().getEventBus();
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String credReqName)
	{
		getContent().removeAll();

		CredentialRequirements certificateEntry;
		if(credReqName == null)
		{
			certificateEntry = new CredentialRequirements();
			edit = false;
		}
		else
		{
			certificateEntry = controller.getCredentialRequirements(credReqName);
			edit = true;
		}
		initUI(certificateEntry);
	}

	private void initUI(CredentialRequirements initial)
	{
		Collection<CredentialDefinition> allCredentials = controller.getCredentialRequirementDefinitions();
		TextField name = new TextField();
		name.setReadOnly(edit);
		name.setWidth("var(--vaadin-text-field-medium)");

		TextField description = new TextField();
		description.setWidth("var(--vaadin-text-field-big)");

		TwinColSelect<String> requiredCredentials = new TwinColSelect<>();
		requiredCredentials.setLabel(msg.getMessage("CredentialRequirements.available") + " -> " + msg.getMessage("CredentialRequirements.chosen"));
		requiredCredentials.setItems(allCredentials.stream().map(CredentialDefinition::getName));
		requiredCredentials.setWidth("30em");
		requiredCredentials.setHeight("15em");

		FormLayout formLayout = new FormLayout();
		formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		formLayout.addFormItem(name, msg.getMessage("CredentialRequirements.name"));
		formLayout.addFormItem(description, msg.getMessage("ServiceEditorBase.description"));
		formLayout.addFormItem(requiredCredentials, msg.getMessage("CredentialRequirements.credentials"));

		CredentialRequirements cr = initial == null ? new CredentialRequirements(
				msg.getMessage("CredentialRequirements.defaultName"), "", new HashSet<>()) : initial;
		binder = new Binder<>(CredentialRequirements.class);
		binder.forField(name)
				.asRequired(msg.getMessage("fieldRequired")).bind(CredentialRequirements::getName, CredentialRequirements::setName);
		binder.forField(description).bind(CredentialRequirements::getDescription, CredentialRequirements::setDescription);
		binder.forField(requiredCredentials).bind(CredentialRequirements::getRequiredCredentials, CredentialRequirements::setRequiredCredentials);
		binder.setBean(cr);

		getContent().add(new VerticalLayout(formLayout, createActionLayout()));
	}

	private HorizontalLayout createActionLayout()
	{
		Button cancelButton = new Button(msg.getMessage("cancel"));
		cancelButton.addClickListener(event -> UI.getCurrent().navigate(CredentialRequirementsView.class));
		Button updateButton = new Button(edit ? msg.getMessage("update") : msg.getMessage("create"));
		updateButton.addClickListener(event -> onConfirm());
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		return new HorizontalLayout(cancelButton, updateButton);
	}

	private void onConfirm()
	{
		binder.validate();
		if(binder.isValid())
		{
			CredentialRequirements bean = binder.getBean();
			if(edit)
				controller.updateCredentialRequirements(bean, bus);
			else
				controller.addCredentialRequirements(bean, bus);
			UI.getCurrent().navigate(CredentialRequirementsView.class);
		}
	}
}
