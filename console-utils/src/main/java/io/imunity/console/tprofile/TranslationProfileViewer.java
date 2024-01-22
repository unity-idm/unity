/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.endpoint.common.HtmlLabelFactory;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileMode;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

/**
 * Component allowing to view all information about translation profile.
 */
public class TranslationProfileViewer extends VerticalLayout
{	
	private final MessageSource msg;
	protected Span name;
	protected Span description;
	protected Span mode;
	private FormLayout rules;
	private FormLayout main;
	
	
	public TranslationProfileViewer(MessageSource msg)
	{
		super();
		this.msg = msg;
		initUI();
	}

	protected void initUI()
	{	
		main = new FormLayout();
		name = new Span();
		description = new Span();
		mode = new Span();
		mode.setVisible(false);
		rules = new FormLayout();
		main.addFormItem(name, msg.getMessage("TranslationProfileViewer.name"));
		main.addFormItem(description, msg.getMessage("TranslationProfileViewer.description"));
		main.addFormItem(mode, msg.getMessage("TranslationProfileViewer.mode"));
		main.addFormItem(rules, msg.getMessage("TranslationProfileViewer.rules"));
		add(main);
		setSizeFull();
	}

	public void setInput(TranslationProfile profile, 
			TypesRegistryBase<? extends TranslationActionFactory<?>> registry)
	{       
		setEmpty();
		if (profile == null)
		{
			main.setVisible(false);
			return;
		}
		main.setVisible(true);
		
		name.setText(profile.getName());
		description.setText(profile.getDescription());
		mode.setVisible(false);
		if (!profile.getProfileMode().equals(ProfileMode.DEFAULT))
		{	
			mode.setVisible(true);
			mode.setText(profile.getProfileMode().toString().toLowerCase());
		}
		int i=0;
		for (TranslationRule rule : profile.getRules())
		{
			i++;     
			addField(msg.getMessage("TranslationProfileViewer.ruleCondition", i),
					"TranslationActionPresenter.codeValue", 
					rule.getCondition());
			TranslationActionPresenter action = new TranslationActionPresenter(msg, registry, 
					rule.getAction());
			action.addToLayout(rules);
		}
	}

	protected void addField(String name, String msgKey, Object... unsafeArgs)
	{
		rules.add(HtmlLabelFactory.getHtmlLabel(msg, name, msgKey, unsafeArgs));
	}
	
	protected void setEmpty()
	{
		rules.removeAll();
		name.setText("");
		description.setText("");
		mode.setText("");
	}
}
