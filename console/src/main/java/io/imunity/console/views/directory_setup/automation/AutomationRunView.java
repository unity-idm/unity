/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.automation;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.tprofile.ActionEditor;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.directory_setup.automation.mvel.MVELExpressionField;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.bulkops.EntityMVELContextKey;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;

import javax.annotation.security.PermitAll;

@PermitAll
@Breadcrumb(key = "edit")
@Route(value = "/automation/run", layout = ConsoleMenu.class)
public class AutomationRunView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final AutomationController controller;
	private final NotificationPresenter notificationPresenter;
	private Binder<TranslationRule> binder;

	AutomationRunView(MessageSource msg, AutomationController controller, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.controller = controller;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String ruleId) {
		getContent().removeAll();

		TranslationRule translationRule;
		if(ruleId == null)
		{
			translationRule = new TranslationRule("status == 'disabled'", null);
		}
		else
		{
			translationRule = controller.getScheduledRule(ruleId);
		}
		initUI(translationRule);
	}

	protected void initUI(TranslationRule translationRule)
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		MVELExpressionField condition = new MVELExpressionField(msg, "",
				msg.getMessage("MVELExpressionField.conditionDesc"),
				MVELExpressionContext.builder().withTitleKey("RuleEditor.conditionTitle")
						.withEvalToKey("MVELExpressionField.evalToBoolean").withVars(EntityMVELContextKey.toMap())
						.build());
		ActionEditor actionEditor = controller.getActionEditor(translationRule);
		binder = new Binder<>(TranslationRule.class);
		condition.configureBinding(binder, "condition", true);
		binder.setBean(translationRule);

		main.addFormItem(condition, msg.getMessage("RuleEditor.condition"));
		actionEditor.addToLayout(main);

		getContent().add(new VerticalLayout(main, createActionLayout()));
	}

	private HorizontalLayout createActionLayout()
	{
		Button cancelButton = new Button(msg.getMessage("cancel"));
		cancelButton.addClickListener(event -> UI.getCurrent().navigate(AutomationView.class));
		Button updateButton = new Button(msg.getMessage("RunImmediateView.run"));
		updateButton.addClickListener(event -> onConfirm());
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		return new HorizontalLayout(cancelButton, updateButton);
	}

	private void onConfirm()
	{
		TranslationRule rule = binder.getBean();
		controller.applyRule(rule);
		notificationPresenter.showSuccess(msg.getMessage("RunImmediateView.actionInvoked"), "");
		UI.getCurrent().navigate(AutomationView.class);
	}

}
