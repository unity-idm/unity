/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.tprofile;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

import java.util.List;

import static io.imunity.console.tprofile.HtmlLabelFactory.getHtmlLabel;

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
			action.addToLayout(rules);
		}
	}

	private void addField(String name, String msgKey, Object... unsafeArgs)
	{
		rules.add(getHtmlLabel(msg, name, msgKey, unsafeArgs));
	}
}
