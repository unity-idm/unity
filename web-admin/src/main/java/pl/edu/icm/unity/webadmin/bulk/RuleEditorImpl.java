/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.bulk;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;

import pl.edu.icm.unity.server.bulkops.EntityAction;
import pl.edu.icm.unity.server.bulkops.ProcessingRule;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.ActionEditor;
import pl.edu.icm.unity.webadmin.tprofile.MVELExpressionField;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.FormValidator;

/**
 * Edit component of an immediate {@link ProcessingRule}
 * @author K. Benedyczak
 */
public class RuleEditorImpl extends CustomComponent implements RuleEditor<ProcessingRule>
{
	protected UnityMessageSource msg;

	protected MVELExpressionField condition;
	protected ActionEditor actionEditor;

	private FormLayout main;
	
	public RuleEditorImpl(UnityMessageSource msg, ActionEditor actionEditor)
	{
		this.msg = msg;
		this.actionEditor = actionEditor;
		initUI();
	}
	
	public void setInput(ProcessingRule rule)
	{
		condition.setValue(rule.getCondition());
		actionEditor.setInput(rule.getAction());
	}

	protected void initUI()
	{
		main = new FormLayout();
		setCompositionRoot(main);
		
		condition = new MVELExpressionField(msg, msg.getMessage("RuleEditor.condition"),
				msg.getMessage("MVELExpressionField.conditionDesc"));
		condition.setValue("status == 'DISABLED'");
		condition.setValidationVisible(true);
		
		main.addComponents(condition);
		actionEditor.addToLayout(main);
	}

	@Override
	public ProcessingRule getRule() throws FormValidationException
	{
		new FormValidator(main).validate();
		return new ProcessingRule(condition.getValue(), (EntityAction) actionEditor.getAction());
	}
}
