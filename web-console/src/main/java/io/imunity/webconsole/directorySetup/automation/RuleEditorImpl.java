/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directorySetup.automation;

import com.vaadin.data.Binder;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;

import io.imunity.webconsole.tprofile.ActionEditor;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.bulkops.EntityMVELContextKey;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.mvel.MVELExpressionField;

/**
 * Edit component of an immediate {@link ProcessingRule}
 * 
 * @author K. Benedyczak
 */
class RuleEditorImpl extends CustomComponent implements RuleEditor<TranslationRule>
{
	protected MessageSource msg;

	protected MVELExpressionField condition;
	protected ActionEditor actionEditor;
	private Binder<TranslationRule> binder;

	private FormLayout main;

	RuleEditorImpl(MessageSource msg, ActionEditor actionEditor)
	{
		this.msg = msg;
		this.actionEditor = actionEditor;
		initUI();
	}

	void setInput(TranslationRule rule)
	{
		binder.setBean(rule);
		actionEditor.setInput(rule.getAction());
	}

	protected void initUI()
	{
		main = new FormLayout();
		setCompositionRoot(main);

		condition = new MVELExpressionField(msg, msg.getMessage("RuleEditor.condition"),
				msg.getMessage("MVELExpressionField.conditionDesc"),
				MVELExpressionContext.builder().withTitleKey("RuleEditor.conditionTitle")
						.withEvalToKey("MVELExpressionField.evalToBoolean").withVars(EntityMVELContextKey.toMap())
						.build());
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
