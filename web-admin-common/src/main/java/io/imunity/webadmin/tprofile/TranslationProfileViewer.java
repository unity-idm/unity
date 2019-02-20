/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.tprofile;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.types.translation.ProfileMode;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

/**
 * Component allowing to view all information about translation profile.
 * @author P. Piernik
 * 
 */
public class TranslationProfileViewer extends VerticalLayout
{	
	private UnityMessageSource msg;
	protected Label name;
	protected Label description;
	protected Label mode;
	private FormLayout rules;
	private FormLayout main;
	
	
	public TranslationProfileViewer(UnityMessageSource msg)
	{
		super();
		this.msg = msg;
		initUI();
	}

	protected void initUI()
	{	
		main = new CompactFormLayout();
		name = new Label();
		name.setCaption(msg.getMessage("TranslationProfileViewer.name"));
		description = new Label();
		description.setCaption(msg.getMessage("TranslationProfileViewer.description"));	
		mode = new Label();
		mode.setCaption(msg.getMessage("TranslationProfileViewer.mode"));
		mode.setVisible(false);
		rules = new CompactFormLayout();
		rules.setMargin(false);
		rules.setSpacing(false);
		Label rulesLabel = new Label();
		rulesLabel.setCaption(msg.getMessage("TranslationProfileViewer.rules"));
		main.addComponents(name, description, mode, rulesLabel, rules);
		addComponent(main);
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
		
		name.setValue(profile.getName());
		description.setValue(profile.getDescription());
		mode.setVisible(false);
		if (!profile.getProfileMode().equals(ProfileMode.DEFAULT))
		{	
			mode.setVisible(true);
			mode.setValue(profile.getProfileMode().toString().toLowerCase());	
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
		HtmlLabel val = new HtmlLabel(msg);
		val.setCaption(name);
		val.setHtmlValue(msgKey, unsafeArgs);
		rules.addComponent(val);
	}
	
	protected void setEmpty()
	{
		rules.removeAllComponents();
		name.setValue("");
		description.setValue("");
		mode.setValue("");
	}
}
