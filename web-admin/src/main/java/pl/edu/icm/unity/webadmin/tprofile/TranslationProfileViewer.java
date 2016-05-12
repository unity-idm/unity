/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import pl.edu.icm.unity.server.registries.TypesRegistryBase;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.TranslationActionInstance;
import pl.edu.icm.unity.server.translation.TranslationProfileInstance;
import pl.edu.icm.unity.server.translation.TranslationRuleInstance;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

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
	protected Label name;
	protected Label description;
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
		description.setReadOnly(true);
		description.setCaption(msg.getMessage("TranslationProfileViewer.description"));
		rules = new CompactFormLayout();
		rules.setMargin(false);
		rules.setSpacing(false);
		Label rulesLabel = new Label();
		rulesLabel.setCaption(msg.getMessage("TranslationProfileViewer.rules"));
		main.addComponents(name, description, rulesLabel, rules);
		addComponent(main);
		setSizeFull();
	}

	public <T extends TranslationActionInstance, R extends TranslationRuleInstance<T>> 
			void setInput(TranslationProfileInstance<T, R> profile, 
					TypesRegistryBase<? extends TranslationActionFactory> registry)
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
		for (R rule : profile.getRuleInstances())
		{
			i++;     
			addField(msg.getMessage("TranslationProfileViewer.ruleCondition", i),
					"TranslationActionPresenter.codeValue", 
					rule.getCondition());
			TranslationActionPresenter<T> action = new TranslationActionPresenter<T>(msg, registry, 
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
	}
}
