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
import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile.ProfileMode;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.RuleComponent.Callback;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * Component to edit or add translation profile
 * 
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
	private FormLayout rulesLayout;
	private List<RuleComponent> rules;
	public TranslationProfileEditor(UnityMessageSource msg,
			TranslationActionsRegistry registry, TranslationProfile toEdit)
	{
		super();
		this.msg = msg;
		this.registry = registry;
		this.rules = new ArrayList<RuleComponent>();
		editMode = toEdit != null;
		initUI(toEdit);
	}

	private void initUI(TranslationProfile toEdit)
	{
		rulesLayout = new FormLayout();
		rulesLayout.setImmediate(true);
		rulesLayout.setSpacing(false);
		rulesLayout.setMargin(false);

		name = new RequiredTextField(msg);
		name.setCaption(msg.getMessage("TranslationProfileEditor.name"));
		name.setSizeFull();
		name.setValidationVisible(false);
		description = new DescriptionTextArea(
				msg.getMessage("TranslationProfileEditor.description"));

		if (editMode)
		{
			name.setValue(toEdit.getName());
			name.setReadOnly(true);
			description.setValue(toEdit.getDescription());
			for (TranslationRule trule : toEdit.getRules())
			{
				addRuleComponent(trule);

			}
		} else
		{
			name.setValue(msg.getMessage("TranslationProfileEditor.defaultName"));
			addRuleComponent(null);
		}

		HorizontalLayout hl = new HorizontalLayout();
		hl.setSpacing(true);
		Button addRule = new Button();
		addRule.setDescription(msg.getMessage("TranslationProfileEditor.newRule"));
		addRule.setIcon(Images.add.getResource());
		addRule.addStyleName(Reindeer.BUTTON_LINK);
		addRule.addStyleName(Styles.toolbarButton.toString());
		addRule.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				addRuleComponent(null);
			}
		});

		Label t = new Label(msg.getMessage("TranslationProfileEditor.rules"));
		hl.addComponents(t, addRule);

		FormLayout main = new FormLayout();
		main.addComponents(name, description);
		main.setSizeFull();

		VerticalLayout wrapper = new VerticalLayout();
		wrapper.addComponents(main, hl, rulesLayout);
		wrapper.setMargin(false);
		wrapper.setSpacing(false);

		addComponents(wrapper);
		refreshRules();
	}

	private void addRuleComponent(TranslationRule trule)
	{
		RuleComponent r = new RuleComponent(msg, registry, trule, new Callback()
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

			@Override
			public boolean moveTop(RuleComponent rule)
			{
				int position = getRulePosition(rule);
				rules.remove(position);
				rules.add(0, rule);
				refreshRules();
				return true;
			}

			@Override
			public boolean moveBottom(RuleComponent rule)
			{
				int position = getRulePosition(rule);
				rules.remove(position);
				rules.add(rule);
				refreshRules();
				return true;
			}
		});
		
		rules.add(r);
		if (trule == null)
		{			
			r.setFocus();
		}

		refreshRules();
	}

	private int getRulePosition(RuleComponent toCheck)
	{
		return rules.indexOf(toCheck);
	}

	protected void refreshRules()
	{
		rulesLayout.removeAllComponents();
		if (rules.size() == 0)
			return;

		for (RuleComponent r : rules)
		{
			r.setUpVisible(true);
			r.setDownVisible(true);
			if (rules.size() > 2)
			{
				r.setTopVisible(true);
				r.setBottomVisible(true);
			}else
			{
				r.setTopVisible(false);
				r.setBottomVisible(false);
			}	
		}
		rules.get(0).setUpVisible(false);
		rules.get(0).setTopVisible(false);
		rules.get(rules.size() - 1).setDownVisible(false);
		rules.get(rules.size() - 1).setBottomVisible(false);		
		for (RuleComponent r : rules)
		{
			rulesLayout.addComponent(r);
		}
	}

	public TranslationProfile getProfile()
	{
		int nvalidr= 0;
		for (RuleComponent cr : rules)
		{
			if (!cr.validateRule())
			{
				nvalidr++;
			}
		}	
		name.setValidationVisible(true);
		if (!(name.isValid() && nvalidr == 0))
		{
			return null;
		}
		String n = name.getValue();
		String desc = description.getValue();
		List<TranslationRule> trules = new ArrayList<TranslationRule>();
		for (RuleComponent cr : rules)
		{
			TranslationRule r = cr.getRule();
			if (r != null)
			{
				trules.add(r);
			}

		}
		TranslationProfile profile = new TranslationProfile(n, trules, ProfileMode.UPDATE_ONLY);
		profile.setDescription(desc);
		return profile;
	}
}
