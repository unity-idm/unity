/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.bulk;

import com.vaadin.data.Binder;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;

import io.imunity.webadmin.tprofile.ActionEditor;
import pl.edu.icm.unity.engine.api.bulkops.EntityAction;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.mvel.MVELExpressionArea;

/**
 * Edit component of a {@link ScheduledProcessingRuleParam}
 * @author K. Benedyczak
 */
public class ScheduledRuleParamEditorImpl extends CustomComponent implements RuleEditor<ScheduledProcessingRuleParam>
{
	protected UnityMessageSource msg;

	protected MVELExpressionArea condition;
	protected ActionEditor actionEditor;
	protected CronExpressionField cronExpression;
	private Binder<ScheduledProcessingRuleParam> binder;
	
	private FormLayout main;
	
	
	public ScheduledRuleParamEditorImpl(UnityMessageSource msg, ActionEditor actionEditor)
	{
		this.msg = msg;
		this.actionEditor = actionEditor;
		initUI();
	}

	public void setInput(ScheduledProcessingRuleParam toEdit)
	{
		binder.setBean(toEdit);
		actionEditor.setInput(toEdit.getAction());
	}
	
	protected void initUI()
	{
		main = new FormLayout();
		setCompositionRoot(main);

		cronExpression = new CronExpressionField(msg,
				msg.getMessage("RuleEditor.cronExpression"));
		condition = new MVELExpressionArea(msg, msg.getMessage("RuleEditor.condition"),
				msg.getMessage("MVELExpressionField.conditionDesc"));
		condition.setWidth(100, Unit.PERCENTAGE);
		binder = new Binder<>(ScheduledProcessingRuleParam.class);
		condition.configureBinding(binder, "condition", true);
		cronExpression.configureBinding(binder, "cronExpression");
		binder.setBean(new ScheduledProcessingRuleParam("status == 'DISABLED'", null,
				"0 0 6 * * ?"));
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
