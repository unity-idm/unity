/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.bulk;

import pl.edu.icm.unity.server.bulkops.EntityAction;
import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.ActionEditor;
import pl.edu.icm.unity.webadmin.tprofile.MVELExpressionField;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.FormValidator;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;

/**
 * Edit component of a {@link ScheduledProcessingRuleParam}
 * @author K. Benedyczak
 */
public class ScheduledRuleParamEditorImpl extends CustomComponent implements RuleEditor<ScheduledProcessingRuleParam>
{
	protected UnityMessageSource msg;

	protected MVELExpressionField condition;
	protected ActionEditor actionEditor;
	protected CronExpressionField cronExpression;

	private FormLayout main;
	
	
	public ScheduledRuleParamEditorImpl(UnityMessageSource msg, ActionEditor actionEditor)
	{
		this.msg = msg;
		this.actionEditor = actionEditor;
		initUI();
	}

	public void setInput(ScheduledProcessingRuleParam toEdit)
	{
		cronExpression.setValue(toEdit.getCronExpression());
		condition.setValue(toEdit.getCondition());
		actionEditor.setInput(toEdit.getAction());
	}
	
	protected void initUI()
	{
		main = new FormLayout();
		setCompositionRoot(main);
		
		cronExpression = new CronExpressionField(msg, msg.getMessage("RuleEditor.cronExpression"));
		cronExpression.setValue("0 0 6 * * ?");
		
		condition = new MVELExpressionField(msg, msg.getMessage("RuleEditor.condition"),
				msg.getMessage("MVELExpressionField.conditionDesc"));
		condition.setValue("status == 'DISABLED'");
		condition.setValidationVisible(true);
		
		main.addComponents(cronExpression, condition);
		
		actionEditor.addToLayout(main);
	}

	@Override
	public ScheduledProcessingRuleParam getRule() throws FormValidationException
	{
		new FormValidator(main).validate();
		return new ScheduledProcessingRuleParam(condition.getValue(), 
				(EntityAction) actionEditor.getAction(),
				cronExpression.getValue());
	}
}
