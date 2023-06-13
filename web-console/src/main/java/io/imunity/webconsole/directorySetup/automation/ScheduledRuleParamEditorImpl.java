/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directorySetup.automation;

import com.vaadin.data.Binder;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;

import io.imunity.webconsole.tprofile.ActionEditor;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.bulkops.EntityAction;
import pl.edu.icm.unity.engine.api.bulkops.EntityMVELContextKey;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.mvel.MVELExpressionField;

/**
 * Edit component of a {@link ScheduledProcessingRuleParam}
 * @author K. Benedyczak
 */
class ScheduledRuleParamEditorImpl extends CustomComponent implements RuleEditor<ScheduledProcessingRuleParam>
{
	protected MessageSource msg;

	protected MVELExpressionField condition;
	protected ActionEditor actionEditor;
	protected CronExpressionField cronExpression;
	private Binder<ScheduledProcessingRuleParam> binder;
	
	private FormLayout main;
	
	
	ScheduledRuleParamEditorImpl(MessageSource msg, ActionEditor actionEditor)
	{
		this.msg = msg;
		this.actionEditor = actionEditor;
		initUI();
	}

	void setInput(ScheduledProcessingRuleParam toEdit)
	{
		binder.setBean(toEdit);
		actionEditor.setInput(toEdit.getAction());
	}
	
	protected void initUI()
	{
		main = new FormLayout();
		setCompositionRoot(main);

		cronExpression = new CronExpressionField(msg, msg.getMessage("RuleEditor.cronExpression"));
		condition = new MVELExpressionField(msg, msg.getMessage("RuleEditor.condition"),
				msg.getMessage("MVELExpressionField.conditionDesc"),
				MVELExpressionContext.builder().withTitleKey("RuleEditor.conditionTitle")
						.withEvalToKey("MVELExpressionField.evalToBoolean").withVars(EntityMVELContextKey.toMap())
						.build());

		condition.setWidth(100, Unit.PERCENTAGE);
		binder = new Binder<>(ScheduledProcessingRuleParam.class);
		condition.configureBinding(binder, "condition", true);
		cronExpression.configureBinding(binder, "cronExpression");
		binder.setBean(new ScheduledProcessingRuleParam("status == 'DISABLED'", null, "0 0 6 * * ?"));
		main.addComponents(cronExpression, condition);

		actionEditor.addToLayout(main);
	}

	@Override
	public ScheduledProcessingRuleParam getRule() throws FormValidationException
	{
		if (!binder.isValid())
		{
			binder.validate();
			throw new FormValidationException();
		}
		ScheduledProcessingRuleParam rule = binder.getBean();
		rule.setTranslationAction((EntityAction) actionEditor.getAction());
		return rule;
	}
}
