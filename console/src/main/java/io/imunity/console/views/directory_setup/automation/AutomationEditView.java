/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.automation;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.tprofile.ActionEditor;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.authentication.realms.RealmEditView;
import io.imunity.console.views.directory_setup.automation.mvel.MVELExpressionField;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.bulkops.EntityMVELContextKey;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;

import jakarta.annotation.security.PermitAll;
import java.util.Optional;

import static io.imunity.console.views.EditViewActionLayoutFactory.createActionLayout;

@PermitAll
@Route(value = "/automation/edit", layout = ConsoleMenu.class)
public class AutomationEditView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final AutomationController controller;
	private boolean edit;
	private String id;
	private Binder<ScheduledProcessingRuleParam> binder;
	private BreadCrumbParameter breadCrumbParameter;


	AutomationEditView(MessageSource msg, AutomationController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String ruleId)
	{
		getContent().removeAll();
		id = ruleId;

		ScheduledProcessingRuleParam translationRule;
		if(ruleId == null)
		{
			translationRule = new ScheduledProcessingRuleParam("status == 'disabled'", null, "0 0 6 * * ?");
			breadCrumbParameter = new BreadCrumbParameter(null, msg.getMessage("new"));
			edit = false;
		}
		else
		{
			translationRule = controller.getScheduledRule(ruleId);
			breadCrumbParameter = new BreadCrumbParameter(null, msg.getMessage("edit"));
			edit = true;
		}
		initUI(translationRule);
	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	protected void initUI(ScheduledProcessingRuleParam translationRule)
	{
		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

		CronExpressionField cronExpression = new CronExpressionField(msg, "");
		MVELExpressionField condition = new MVELExpressionField(msg, "",
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

		getContent().add(new VerticalLayout(main, createActionLayout(msg, edit, AutomationView.class, this::onConfirm)));
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
