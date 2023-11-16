/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.credentials;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import io.imunity.vaadin.elements.LocalizedSpan;
import io.imunity.vaadin.elements.Panel;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionViewer;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.Optional;


@PermitAll
@Route(value = "/credentials/info", layout = ConsoleMenu.class)
public class CredentialsInfoView extends ConsoleViewComponent
{
	private final CredentialsController credentialsController;
	private final CredentialEditorRegistry credentialEditorReg;
	private final MessageSource msg;

	private BreadCrumbParameter breadCrumbParameter;

	CredentialsInfoView(MessageSource msg, CredentialsController credentialsController, CredentialEditorRegistry credentialEditorReg)
	{
		this.credentialsController = credentialsController;
		this.credentialEditorReg = credentialEditorReg;
		this.msg = msg;
	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String credentialName)
	{
		getContent().removeAll();

		breadCrumbParameter = new BreadCrumbParameter(credentialName, credentialName);
		CredentialDefinition credential = credentialsController.getCredential(credentialName);
		initUI(credential);
	}

	private void initUI(CredentialDefinition credential)
	{
		FormLayout formLayout = new FormLayout();
		formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		Span name = new Span(credential.getName());
		formLayout.addFormItem(name, msg.getMessage("CredentialDefinition.name"));

		LocalizedSpan displayedName = new LocalizedSpan(credential.getDisplayedName().getLocalizedMap());
		formLayout.addFormItem(displayedName, msg.getMessage("displayedNameF"));

		LocalizedSpan description = new LocalizedSpan(credential.getDescription().getLocalizedMap());
		formLayout.addFormItem(description, msg.getMessage("descriptionF"));

		Span type = new Span(credential.getTypeId());
		formLayout.addFormItem(type, msg.getMessage("CredentialDefinition.type"));

		Panel typeSpecific = new Panel(msg.getMessage("CredentialDefinition.typeSettings"));
		CredentialDefinitionViewer viewer =
				credentialEditorReg.getFactory(credential.getTypeId()).creteCredentialDefinitionViewer();
		typeSpecific.add(viewer.getViewer(credential.getConfiguration()));
		formLayout.addFormItem(typeSpecific, "");

		Button cancelButton = new Button(msg.getMessage("cancel"));
		cancelButton.addClickListener(event -> UI.getCurrent().navigate(CredentialsView.class));
		cancelButton.setWidth("13em");
		getContent().add(new VerticalLayout(formLayout, cancelButton));
	}
}
