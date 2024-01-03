/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console_utils.tprofile;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

import java.util.List;

import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

/**
 * Presents rules from translation profile
 */
public class TranslationRulesPresenter extends VerticalLayout
{
	private final MessageSource msg;
	private FormLayout rules;
	private final TypesRegistryBase<? extends TranslationActionFactory<?>> registry;

	public TranslationRulesPresenter(MessageSource msg,
			TypesRegistryBase<? extends TranslationActionFactory<?>> registry)
	{
		super();
		this.msg = msg;
		this.registry = registry;
		initUI();
	}

	protected void initUI()
	{
		rules = new FormLayout();
		rules.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		rules.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		setSizeFull();
		add(rules);
	}

	public void setInput(List<? extends TranslationRule> profileRules)
	{
		rules.removeAll();
		int i = 0;
		for (TranslationRule rule : profileRules)
		{
			i++;
			addField(msg.getMessage("TranslationRulesPresenter.ruleCondition", i),
					"TranslationRulesPresenter.codeValue", rule.getCondition());
			TranslationActionPresenter action = new TranslationActionPresenter(msg, registry,
					rule.getAction());
			action.addToFormLayout(rules);
		}
	}

	private void addField(String name, String msgKey, Object... unsafeArgs)
	{
		rules.addFormItem(new Html(msg.getMessageNullArg(msgKey, unsafeArgs)), name);
	}
}
