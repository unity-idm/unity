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

/**
 * Edit component of an immediate {@link ProcessingRule}
 * @author K. Benedyczak
 */
public class RuleEditorImpl extends CustomComponent implements RuleEditor<ProcessingRule>
{
	protected UnityMessageSource msg;

	protected MVELExpressionField condition;
	protected ActionEditor<EntityAction> actionEditor;
	
	public RuleEditorImpl(UnityMessageSource msg, ActionEditor<EntityAction> actionEditor)
	{
		this.msg = msg;
		this.actionEditor = actionEditor;
		initUI();
	}

	protected void initUI()
	{
		FormLayout main = new FormLayout();
		setCompositionRoot(main);
		
		condition = new MVELExpressionField(msg, msg.getMessage("RuleEditor.condition"));
		main.addComponents(condition);
		actionEditor.iterator().forEachRemaining(c -> main.addComponent(c));
	}

	@Override
	public ProcessingRule getRule() throws FormValidationException
	{
		return new ProcessingRule(condition.getValue(), (EntityAction) actionEditor.getAction());
	}
}
