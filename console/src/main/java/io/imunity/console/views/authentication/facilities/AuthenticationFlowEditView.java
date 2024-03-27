/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.facilities;

import static io.imunity.console.views.EditViewActionLayoutFactory.createActionLayout;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.html.Span;
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
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.mvel.MVELExpressionField;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.DynamicPolicyConfigurationMVELContextKey;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;

@PermitAll
@Route(value = "/facilities/authentication-flow", layout = ConsoleMenu.class)
public class AuthenticationFlowEditView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final AuthenticationFlowsController flowsController;
	private boolean edit;
	private BreadCrumbParameter breadCrumbParameter;
	private Binder<AuthenticationFlowDefinition> binder;
	private final HtmlTooltipFactory htmlTooltipFactory;

	AuthenticationFlowEditView(MessageSource msg, AuthenticationFlowsController flowsController, HtmlTooltipFactory htmlTooltipFactory)
	{
		this.msg = msg;
		this.flowsController = flowsController;
		this.htmlTooltipFactory = htmlTooltipFactory;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String flowName)
	{
		getContent().removeAll();

		AuthenticationFlowEntry definition;
		if(flowName == null)
		{
			AuthenticationFlowDefinition flow = new AuthenticationFlowDefinition("", AuthenticationFlowDefinition.Policy.REQUIRE, Set.of(), List.of(), null);
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
		name.setWidth(TEXT_FIELD_BIG.value());
		name.setReadOnly(edit);

		MultiSelectComboBox<String> firstFactorAuthenticators = new MultiSelectComboBox<>();
		firstFactorAuthenticators.setItems(authenticators);
		firstFactorAuthenticators.setWidth(TEXT_FIELD_BIG.value());

		MultiSelectComboBox<String> secondFactorAuthenticators = new MultiSelectComboBox<>();
		secondFactorAuthenticators.setItems(authenticators);
		secondFactorAuthenticators.setWidth(TEXT_FIELD_BIG.value());

		ComboBox<AuthenticationFlowDefinition.Policy> policy = new ComboBox<>();
		policy.setItems(AuthenticationFlowDefinition.Policy.values());

		MVELExpressionField policyConfig = new MVELExpressionField(msg,
				msg.getMessage("MVELExpressionField.conditionDesc"),
				MVELExpressionContext.builder().withTitleKey("AuthenticationFlow.policyConfigurationTitle")
						.withEvalToKey("MVELExpressionField.evalToBoolean").withVars(DynamicPolicyConfigurationMVELContextKey.toMap())
						.build(), htmlTooltipFactory);
		
		
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
		binder.forField(policy)
				.bind(AuthenticationFlowDefinition::getPolicy, AuthenticationFlowDefinition::setPolicy);
		binder.forField(policyConfig)
				.bind(AuthenticationFlowDefinition::getDynamicPolicyMvelCondition, AuthenticationFlowDefinition::setDynamicPolicyMvelCondition);

		FormLayout mainLayout = new FormLayout();
		mainLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		mainLayout.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		mainLayout.addFormItem(name, msg.getMessage("AuthenticationFlow.name"));
		mainLayout.addFormItem(policy, msg.getMessage("AuthenticationFlow.policy"));
		FormItem formItem = mainLayout.addFormItem(policyConfig, msg.getMessage("AuthenticationFlow.policyConfiguration"));
		formItem.setVisible(false);
		mainLayout.addFormItem(firstFactorAuthenticators, msg.getMessage("AuthenticationFlow.firstFactorAuthenticators"));
		mainLayout.addFormItem(secondFactorAuthenticators, msg.getMessage("AuthenticationFlow.secondFactorAuthenticators"));
		policy.addValueChangeListener(v ->
		{
			formItem.setVisible(v.getValue()
					.equals(Policy.DYNAMIC));
			if (!v.getValue()
					.equals(Policy.DYNAMIC))
			{
				policyConfig.clear();
			}
		});
		binder.setBean(toEdit.flow);

		
		if (!toEdit.endpoints.isEmpty())
		{
			VerticalLayout field = new VerticalLayout(toEdit.endpoints.stream().map(Span::new).toArray(Component[]::new));
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
