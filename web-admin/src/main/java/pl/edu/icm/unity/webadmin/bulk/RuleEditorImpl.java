/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.bulk;

import com.vaadin.data.Binder;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;

import io.imunity.webadmin.tprofile.ActionEditor;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.translation.TranslationRule;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.mvel.MVELExpressionField;

/**
 * Edit component of an immediate {@link ProcessingRule}
 * @author K. Benedyczak
 */
public class RuleEditorImpl extends CustomComponent implements RuleEditor<TranslationRule>
{
	protected UnityMessageSource msg;

	protected MVELExpressionField condition;
	protected ActionEditor actionEditor;
	private Binder<TranslationRule> binder;
	
	private FormLayout main;
	
	public RuleEditorImpl(UnityMessageSource msg, ActionEditor actionEditor)
	{
		this.msg = msg;
		this.actionEditor = actionEditor;
		initUI();
	}
	
	public void setInput(TranslationRule rule)
	{
		binder.setBean(rule);
		actionEditor.setInput(rule.getAction());
	}

	protected void initUI()
	{
		main = new FormLayout();
		setCompositionRoot(main);

		condition = new MVELExpressionField(msg, msg.getMessage("RuleEditor.condition"),
				msg.getMessage("MVELExpressionField.conditionDesc"));
		binder = new Binder<>(TranslationRule.class);
		condition.configureBinding(binder, "condition", true);
		binder.setBean(new TranslationRule("status == 'disabled'", null));

		main.addComponents(condition);
		actionEditor.addToLayout(main);
	}

	@Override
	public TranslationRule getRule() throws FormValidationException
	{
		if (!binder.isValid())
		{
			binder.validate();
			throw new FormValidationException();
		}
		TranslationRule rule = binder.getBean();
		rule.setTranslationAction(actionEditor.getAction());
		return rule;
	}
}
