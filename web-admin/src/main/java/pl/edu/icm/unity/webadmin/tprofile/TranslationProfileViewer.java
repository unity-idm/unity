/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationRule;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Component allowing to view all information about translation profile.
 * @author P. Piernik
 * 
 */
public class TranslationProfileViewer extends VerticalLayout
{	
	private UnityMessageSource msg;
	private Label name;
	private Label description;
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
		main = new FormLayout();
		name = new Label();
		name.setCaption(msg.getMessage("TranslationProfileViewer.name") + ":");
		description = new Label();
		description.setCaption(msg.getMessage("TranslationProfileViewer.description") + ":");
		rules = new FormLayout();
		rules.setMargin(false);
		rules.setSpacing(false);
		Label rulesLabel = new Label();
		rulesLabel.setCaption(msg.getMessage("TranslationProfileViewer.rules") + ":");
		main.addComponents(name, description, rulesLabel, rules);
		addComponent(main);
		setSizeFull();
	}

	public void setInput(TranslationProfile profile)
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
		
		int i=0;
		for (TranslationRule rule : profile.getRules())
		{	i++;
		       
			addRule(String.valueOf(i) + ":  " + msg.getMessage("TranslationProfileViewer.ruleCondition"),
					"<code>" + rule.getCondition().getCondition() + "</code>");
			StringBuilder params = new StringBuilder();
			for (String p : rule.getAction().getParameters())
			{
				if (params.length() > 0)
					params.append(", ");
				params.append(p);
			}
			addRule(msg.getMessage("TranslationProfileViewer.ruleAction"),
					"<code>" + rule.getAction().getName() + "</code>" + " " + params);
		}

	}

	protected void addRule(String name, String value)
	{
		Label val = new Label(value, ContentMode.HTML);
		val.setCaption(name + ":");
		rules.addComponent(val);
	}
	
	protected void setEmpty()
	{
		rules.removeAllComponents();
		name.setValue("");
		description.setValue("");
		
	}

}
