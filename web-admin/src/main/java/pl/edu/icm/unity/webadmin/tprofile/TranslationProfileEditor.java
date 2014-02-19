/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationRule;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.RuleComponent.Callback;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.RequiredTextField;

import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.Reindeer;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.VerticalLayout;

/**
 * Component to edit or add translation profile
 * @author P. Piernik
 *
 */
public class TranslationProfileEditor extends VerticalLayout
{
	private UnityMessageSource msg;
	private TranslationActionsRegistry registry;
	private boolean editMode;
	private AbstractTextField name;
	private DescriptionTextArea description;
	private FormLayout rulesL;
	private List<RuleComponent> rules;
	
	public TranslationProfileEditor(UnityMessageSource msg,
			TranslationActionsRegistry registry, TranslationProfile toEdit)
	{
		super();
		editMode = toEdit != null;
		this.msg = msg;
		this.registry = registry;
		this.rules = new ArrayList<RuleComponent>();
		initUI(toEdit);

	}

	private void initUI(TranslationProfile toEdit)
	{
		
		setWidth(100, Unit.PERCENTAGE);
		setHeight(100, Unit.PERCENTAGE);
		setSpacing(true);
		
		rulesL = new FormLayout();
		rulesL.setImmediate(true);
		rulesL.setSpacing(false);
		rulesL.setMargin(false);
		
		name = new RequiredTextField(msg);
		name.setCaption(msg.getMessage("TranslationProfileEditor.name") + ":");
		name.setSizeFull();
		description = new DescriptionTextArea(
				msg.getMessage("TranslationProfileEditor.description") + ":");
		
		
		if (editMode)
		{	
			name.setValue(toEdit.getName());
			name.setReadOnly(true);
			description.setValue(toEdit.getDescription());
			for (TranslationRule trule:toEdit.getRules())
			{ 
				addRuleComponent(trule, false);
				
			}
		} else
			name.setValue(msg.getMessage("TranslationProfileEditor.defaultName"));
		
		
		HorizontalLayout hl = new HorizontalLayout();
		hl.setMargin(false);
		Button addRule = new Button();
		addRule.setDescription(msg.getMessage("TranslationProfileEditor.newRule"));
		addRule.setIcon(Images.add.getResource());
		addRule.addStyleName(Reindeer.BUTTON_SMALL);
		addRule.addClickListener(new ClickListener()
		{
			
			@Override
			public void buttonClick(ClickEvent event)
			{
				addRuleComponent(null, true);
				
			}
		});
		
		Label t = new Label(msg.getMessage("TranslationProfileEditor.rules") + ":");
		hl.addComponents(t, addRule);
		
		
		refreshRules();
		FormLayout main = new FormLayout();
		main.addComponents(name, description, hl,rulesL);
		main.setSizeFull();
		addComponent(main);
	}

	
	private void addRuleComponent(TranslationRule trule, boolean toStart)
	{
		final RuleComponent r = new RuleComponent(msg, registry, trule, new Callback()
		{

			@Override
			public boolean moveUp(RuleComponent rule)
			{

				int position = getRulePosition(rule);
				if (position != 0)
				{
					Collections.swap(rules, position, position - 1);
				}

				refreshRules();
				return true;
			}

			@Override
			public boolean moveDown(RuleComponent rule)
			{
				int position = getRulePosition(rule);
				if (position != rules.size() - 1)
				{
					Collections.swap(rules, position, position + 1);
				}
				refreshRules();
				return true;
			}

			@Override
			public boolean remove(RuleComponent rule)
			{
				rules.remove(rule);
				refreshRules();
				return true;
			}
		});
		if (toStart)
		{
			rules.add(0, r);
		}

		else
		{
			rules.add(r);
		}
		refreshRules();

	}
	
	
	private int getRulePosition(RuleComponent toCheck)
	{
		return rules.indexOf(toCheck);
		
	}
	
	protected void refreshRules()
	{
		rulesL.removeAllComponents();
		if (rules.size() == 0)
			return;
		
		for(RuleComponent r:rules)
		{
			r.setUpVisible(true);
			r.setDownVisible(true);
		}
		
		rules.get(0).setUpVisible(false);
		rules.get(rules.size()-1).setDownVisible(false);
		
		
		for(RuleComponent r:rules)
		{
			rulesL.addComponent(r);
		}
		
		
		
		
	}

	public TranslationProfile getProfile()
	{
		boolean validated = true;
		for (RuleComponent cr : rules)
		{
			if(!validated)
				continue;
			validated = cr.validateCondition();
		}
		
		if (!validated)
			return null;
		
		String n = name.getValue();
		String desc = description.getValue();

		List<TranslationRule> trules = new ArrayList<TranslationRule>();

		for (RuleComponent cr : rules)
		{
			trules.add(cr.getRule());
		}

		TranslationProfile profile = new TranslationProfile(n, trules);
		profile.setDescription(desc);

		return profile;

	}
	
}
