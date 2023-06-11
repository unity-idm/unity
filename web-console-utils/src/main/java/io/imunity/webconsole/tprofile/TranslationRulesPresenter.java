/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.tprofile;

import java.util.List;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

/**
 * Presents rules from translation profile
 * 
 * @author P.Piernik
 *
 */
public class TranslationRulesPresenter extends CustomComponent
{
	private MessageSource msg;
	private FormLayout rules;
	private TypesRegistryBase<? extends TranslationActionFactory<?>> registry;

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
		rules = new FormLayoutWithFixedCaptionWidth();
		rules.setMargin(false);
		rules.setSpacing(false);
		setSizeFull();
		setCompositionRoot(rules);
	}

	public void setInput(List<? extends TranslationRule> profileRules)
	{
		rules.removeAllComponents();
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
		HtmlLabel val = new HtmlLabel(msg);
		val.setCaption(name);
		val.setHtmlValue(msgKey, unsafeArgs);
		rules.addComponent(val);
	}
}
