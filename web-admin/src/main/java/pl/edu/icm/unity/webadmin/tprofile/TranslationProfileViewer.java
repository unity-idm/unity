/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.authn.remote.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationRule;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

/**
 * Component allowing to view all information about translation profile.
 * @author P. Piernik
 * 
 */
public class TranslationProfileViewer extends VerticalLayout
{	
	private UnityMessageSource msg;
	private TranslationActionsRegistry registry;
	private Label name;
	private TextArea description;
	private FormLayout rules;
	private FormLayout main;
	
	
	public TranslationProfileViewer(UnityMessageSource msg, TranslationActionsRegistry registry)
	{
		super();
		this.msg = msg;
		this.registry = registry;
		initUI();
	}

	protected void initUI()
	{	
		main = new FormLayout();
		name = new Label();
		name.setCaption(msg.getMessage("TranslationProfileViewer.name"));
		description = new DescriptionTextArea();
		description.setReadOnly(true);
		description.setCaption(msg.getMessage("TranslationProfileViewer.description"));
		rules = new FormLayout();
		rules.setMargin(false);
		rules.setSpacing(false);
		Label rulesLabel = new Label();
		rulesLabel.setCaption(msg.getMessage("TranslationProfileViewer.rules"));
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
		description.setRows(profile.getDescription().split("\n").length);
		int i=0;
		for (TranslationRule rule : profile.getRules())
		{
			
			ActionParameterDesc[] pd = null;
			try 
			{
				TranslationActionFactory f = registry.getByName(rule.getAction().
						getActionDescription().getName());
				pd = f.getParameters();
			} catch (IllegalTypeException e)
			{
				
			}
			i++;     
			addField(String.valueOf(i) + ":  " + msg.getMessage("TranslationProfileViewer.ruleCondition"),
					"<code>" + rule.getCondition().getCondition() + "</code>");
			addField(msg.getMessage("TranslationProfileViewer.ruleAction"),
					"<code>" + rule.getAction().getActionDescription().getName() + "</code>");
			String[] par = rule.getAction().getParameters();
			for (int j = 0; j < par.length; j++)
			{
				if (j == 0)
				{
					addField(msg.getMessage("TranslationProfileViewer.ruleActionParameters"),
							pd[j].getName() + " = <code>" + par[j]);
				}else
				{
					addField("", pd[j].getName() + " = <code>" + par[j]);
				}
			}		
		}

	}

	protected void addField(String name, String value)
	{
		Label val = new Label(value, ContentMode.HTML);
		val.setCaption(name);
		rules.addComponent(val);
	}
	
	protected void setEmpty()
	{
		rules.removeAllComponents();
		name.setValue("");
		description.setValue("");
		
	}

}
