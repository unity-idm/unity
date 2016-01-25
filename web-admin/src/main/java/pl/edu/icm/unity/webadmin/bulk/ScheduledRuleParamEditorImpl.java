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

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

/**
 * Edit component of a {@link ScheduledProcessingRuleParam}
 * @author K. Benedyczak
 */
public class ScheduledRuleParamEditorImpl extends CustomComponent implements RuleEditor<ScheduledProcessingRuleParam>
{
	protected UnityMessageSource msg;

	protected MVELExpressionField condition;
	protected ActionEditor<EntityAction> actionEditor;
	protected TextField cronExpression;
	
	
	public ScheduledRuleParamEditorImpl(UnityMessageSource msg, ActionEditor<EntityAction> actionEditor)
	{
		this.msg = msg;
		this.actionEditor = actionEditor;
		initUI();
	}

	protected void initUI()
	{
		FormLayout main = new FormLayout();
		setCompositionRoot(main);
		
		cronExpression = new TextField(msg.getMessage("RuleEditor.cronExpression"));
		
		condition = new MVELExpressionField(msg, msg.getMessage("RuleEditor.condition"));
		
		main.addComponents(cronExpression, condition);
		
		actionEditor.iterator().forEachRemaining(c -> main.addComponent(c));
	}

	@Override
	public ScheduledProcessingRuleParam getRule() throws FormValidationException
	{
		return new ScheduledProcessingRuleParam(condition.getValue(), 
				(EntityAction) actionEditor.getAction(),
				cronExpression.getValue());
	}
}
