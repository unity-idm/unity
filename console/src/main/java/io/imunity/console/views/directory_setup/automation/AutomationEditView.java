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
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.bulkops.EntityMVELContextKey;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;

import javax.annotation.security.PermitAll;

@PermitAll
@Breadcrumb(key = "edit")
@Route(value = "/automation/edit", layout = ConsoleMenu.class)
public class AutomationEditView extends ConsoleViewComponent
{
	private MessageSource msg;
	private AutomationController controller;
	private boolean edit;
	private String id;
	private Binder<ScheduledProcessingRuleParam> binder;

	private MVELExpressionField condition;
	private CronExpressionField cronExpression;

	public AutomationEditView(MessageSource msg, AutomationController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String ruleId) {
		getContent().removeAll();
		id = ruleId;

		ScheduledProcessingRuleParam translationRule;
		if(ruleId == null)
		{
			translationRule = new ScheduledProcessingRuleParam("status == 'disabled'", null, "0 0 6 * * ?");
			edit = false;
		}
		else
		{
			translationRule = controller.getScheduledRule(ruleId);
			edit = true;
		}
		initUI(translationRule);
	}

	protected void initUI(ScheduledProcessingRuleParam translationRule)
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		cronExpression = new CronExpressionField(msg, "");
		condition = new MVELExpressionField(msg, "",
				msg.getMessage("MVELExpressionField.conditionDesc"),
				MVELExpressionContext.builder().withTitleKey("RuleEditor.conditionTitle")
						.withEvalToKey("MVELExpressionField.evalToBoolean").withVars(EntityMVELContextKey.toMap())
						.build());
		ActionEditor actionEditor = controller.getActionEditor(translationRule);
		binder = new Binder<>(ScheduledProcessingRuleParam.class);
		condition.configureBinding(binder, "condition", true);
		cronExpression.configureBinding(binder, "cronExpression");
		binder.setBean(translationRule);

		main.addFormItem(cronExpression, msg.getMessage("RuleEditor.cronExpression"));
		main.addFormItem(condition, msg.getMessage("RuleEditor.condition"));
		actionEditor.addToLayout(main);

		getContent().add(new VerticalLayout(main, createActionLayout()));
	}

	private HorizontalLayout createActionLayout()
	{
		Button cancelButton = new Button(msg.getMessage("cancel"));
		cancelButton.addClickListener(event -> UI.getCurrent().navigate(AutomationView.class));
		Button updateButton = new Button(edit ? msg.getMessage("update") : msg.getMessage("create"));
		updateButton.addClickListener(event -> onConfirm());
		updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		return new HorizontalLayout(cancelButton, updateButton);
	}

	private void onConfirm()
	{
		ScheduledProcessingRuleParam rule = binder.getBean();
		if(!edit)
			controller.scheduleRule(rule);
		else
			controller.updateScheduledRule(id, rule);
		UI.getCurrent().navigate(AutomationView.class);
	}

}
