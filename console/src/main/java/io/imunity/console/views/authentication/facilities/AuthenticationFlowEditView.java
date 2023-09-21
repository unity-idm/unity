/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.facilities;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.message.MessageSource;

import javax.annotation.security.PermitAll;
import java.util.*;

import static io.imunity.console.views.EditViewActionLayoutFactory.createActionLayout;

@PermitAll
@Route(value = "/facilities/authentication-flow", layout = ConsoleMenu.class)
public class AuthenticationFlowEditView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final AuthenticationFlowsController flowsController;
	private boolean edit;
	private BreadCrumbParameter breadCrumbParameter;
	private Binder<AuthenticationFlowDefinition> binder;

	AuthenticationFlowEditView(MessageSource msg, AuthenticationFlowsController flowsController)
	{
		this.msg = msg;
		this.flowsController = flowsController;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String flowName)
	{
		getContent().removeAll();

		AuthenticationFlowEntry definition;
		if(flowName == null)
		{
			AuthenticationFlowDefinition flow = new AuthenticationFlowDefinition("", AuthenticationFlowDefinition.Policy.REQUIRE, Set.of(), List.of());
			definition = new AuthenticationFlowEntry(flow, List.of());
			breadCrumbParameter = new BreadCrumbParameter(null, msg.getMessage("new"));
			edit = false;
		}
		else
		{
			definition = flowsController.getFlow(flowName);
			breadCrumbParameter = new BreadCrumbParameter(flowName, flowName);
			edit = true;
		}
		initUI(definition, flowsController.getAllAuthenticators());
	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	private void initUI(AuthenticationFlowEntry toEdit, List<String> authenticators)
	{
		TextField name = new TextField();
		name.setPlaceholder(msg.getMessage("AuthenticationFlow.defaultName"));
		name.setWidth("var(--vaadin-text-field-big)");
		name.setReadOnly(edit);

		MultiSelectComboBox<String> firstFactorAuthenticators = new MultiSelectComboBox<>();
		firstFactorAuthenticators.setItems(authenticators);
		firstFactorAuthenticators.setWidth("var(--vaadin-text-field-big)");

		MultiSelectComboBox<String> secondFactorAuthenticators = new MultiSelectComboBox<>();
		secondFactorAuthenticators.setItems(authenticators);
		secondFactorAuthenticators.setWidth("var(--vaadin-text-field-big)");

		ComboBox<AuthenticationFlowDefinition.Policy> policy = new ComboBox<>();
		policy.setItems(AuthenticationFlowDefinition.Policy.values());

		binder = new Binder<>(AuthenticationFlowDefinition.class);
		binder.forField(name)
				.withValidator(((value, context) -> value != null && value.contains(" ") ? ValidationResult.error(msg.getMessage("NoSpaceValidator.noSpace")) : ValidationResult.ok()))
				.asRequired(msg.getMessage("fieldRequired"))
				.bind(AuthenticationFlowDefinition::getName, AuthenticationFlowDefinition::setName);
		binder.forField(firstFactorAuthenticators)
				.withValidator((value, context) -> value == null || value.isEmpty() ? ValidationResult.error(msg.getMessage("fieldRequired")) : ValidationResult.ok())
				.asRequired(msg.getMessage("fieldRequired"))
				.bind(AuthenticationFlowDefinition::getFirstFactorAuthenticators, AuthenticationFlowDefinition::setFirstFactorAuthenticators);
		binder.forField(secondFactorAuthenticators)
				.withConverter(c -> (List<String>) new ArrayList<>(c), HashSet::new)
				.bind(AuthenticationFlowDefinition::getSecondFactorAuthenticators, AuthenticationFlowDefinition::setSecondFactorAuthenticators);
		binder.forField(policy)
				.bind(AuthenticationFlowDefinition::getPolicy, AuthenticationFlowDefinition::setPolicy);
		binder.setBean(toEdit.flow);

		FormLayout mainLayout = new FormLayout();
		mainLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		mainLayout.addClassName("big-vaadin-form-item");
		mainLayout.addFormItem(name, msg.getMessage("AuthenticationFlow.name"));
		mainLayout.addFormItem(policy, msg.getMessage("AuthenticationFlow.policy"));
		mainLayout.addFormItem(firstFactorAuthenticators, msg.getMessage("AuthenticationFlow.firstFactorAuthenticators"));
		mainLayout.addFormItem(secondFactorAuthenticators, msg.getMessage("AuthenticationFlow.secondFactorAuthenticators"));
		if (!toEdit.endpoints.isEmpty())
		{
			VerticalLayout field = new VerticalLayout(toEdit.endpoints.stream().map(Label::new).toArray(Component[]::new));
			field.setPadding(false);
			mainLayout.addFormItem(field, msg.getMessage("AuthenticationFlow.endpoints"));
		}

		getContent().add(new VerticalLayout(mainLayout, createActionLayout(msg, edit, FacilitiesView.class, this::onConfirm)));
	}

	private void onConfirm()
	{
		binder.validate();
		if(binder.isValid())
		{
			AuthenticationFlowDefinition bean = binder.getBean();
			if(edit)
				flowsController.updateFlow(bean);
			else
				flowsController.addFlow(bean);
			UI.getCurrent().navigate(FacilitiesView.class);
		}
	}
}
