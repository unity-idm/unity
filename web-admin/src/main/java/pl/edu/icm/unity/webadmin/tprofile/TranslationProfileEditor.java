/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.registries.TypesRegistryBase;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.TranslationProfileInstance;
import pl.edu.icm.unity.server.translation.TranslationRuleInstance;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;
import pl.edu.icm.unity.webadmin.tprofile.ActionParameterComponentFactory.Provider;
import pl.edu.icm.unity.webadmin.tprofile.RuleComponent.Callback;
import pl.edu.icm.unity.webadmin.tprofile.StartStopButton.ClickStartEvent;
import pl.edu.icm.unity.webadmin.tprofile.StartStopButton.ClickStopEvent;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.FormValidationException;
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

/**
 * Generic component to edit or add translation profile of any type
 * 
 * @author P. Piernik
 * 
 */
public class TranslationProfileEditor extends VerticalLayout
{
	protected UnityMessageSource msg;
	protected ProfileType type;
	protected TypesRegistryBase<? extends TranslationActionFactory> registry;
	protected AbstractTextField name;
	protected DescriptionTextArea description;
	protected FormLayout rulesLayout;
	protected List<RuleComponent> rules;
	
	private RemotelyAuthenticatedInput remoteAuthnInput;
	private StartStopButton testProfileButton;
	private Provider actionComponentProvider;
	
	public TranslationProfileEditor(UnityMessageSource msg,
			TypesRegistryBase<? extends TranslationActionFactory> registry, ProfileType type, 
			Provider actionComponentProvider) throws EngineException
	{
		super();
		this.msg = msg;
		this.registry = registry;
		this.type = type;
		this.actionComponentProvider = actionComponentProvider;
		this.rules = new ArrayList<>();
		initUI();
	}

	public void setValue(TranslationProfileInstance<?, ?> toEdit)
	{
		name.setReadOnly(false);
		name.setValue(toEdit.getName());
		name.setReadOnly(true);
		description.setValue(toEdit.getDescription());
		for (TranslationRuleInstance<?> trule : toEdit.getRuleInstances())
		{
			addRuleComponent(trule);
		}
	}
	
	public TranslationProfile getProfile() throws FormValidationException
	{
		int nvalidr= 0;
		for (RuleComponent cr : rules)
		{
			if (!cr.validateRule())
				nvalidr++;
		}	
		name.setValidationVisible(true);
		if (!name.isValid() || nvalidr != 0)
			throw new FormValidationException();
		List<TranslationRule> trules = new ArrayList<>();
		for (RuleComponent cr : rules)
		{
			TranslationRule r = cr.getRule();
			if (r != null)
				trules.add(r);
		}
		return new TranslationProfile(name.getValue(), description.getValue(), 
				type, trules);
	}
	
	protected void initUI()
	{
		rulesLayout = new CompactFormLayout();
		rulesLayout.setImmediate(true);
		rulesLayout.setSpacing(false);
		rulesLayout.setMargin(false);

		name = new RequiredTextField(msg);
		name.setCaption(msg.getMessage("TranslationProfileEditor.name"));
		name.setSizeFull();
		name.setValidationVisible(false);
		description = new DescriptionTextArea(
				msg.getMessage("TranslationProfileEditor.description"));

		name.setValue(msg.getMessage("TranslationProfileEditor.defaultName"));

		HorizontalLayout hl = new HorizontalLayout();
		hl.setSpacing(true);
		Button addRule = new Button();
		addRule.setDescription(msg.getMessage("TranslationProfileEditor.newRule"));
		addRule.setIcon(Images.add.getResource());
		addRule.addStyleName(Styles.vButtonLink.toString());
		addRule.addStyleName(Styles.toolbarButton.toString());
		addRule.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				addRuleComponent(null);
			}
		});
		
		testProfileButton = new StartStopButton();
		testProfileButton.setVisible(false);
		testProfileButton.setDescription(msg.getMessage("TranslationProfileEditor.testProfile"));
		testProfileButton.addClickListener(new StartStopButton.StartStopListener() 
		{
			@Override
			public void onStop(ClickStopEvent event) 
			{
				clearTestResults();
			}
			
			@Override
			public void onStart(ClickStartEvent event) 
			{
				testRules();
			}
		});

		Label t = new Label(msg.getMessage("TranslationProfileEditor.rules"));
		hl.addComponents(t, addRule, testProfileButton);

		FormLayout main = new CompactFormLayout();
		main.addComponents(name, description);
		main.setSizeFull();

		VerticalLayout wrapper = new VerticalLayout();
		wrapper.addComponents(main, hl, rulesLayout);
		wrapper.setMargin(false);
		wrapper.setSpacing(false);

		addComponents(wrapper);
		refreshRules();
	}

	protected void testRules() 
	{
		for (RuleComponent rule : rules)
		{
			rule.test(remoteAuthnInput);
		}
	}

	protected void clearTestResults() 
	{
		for (RuleComponent rule : rules)
		{
			rule.clearTestResult();
		}		
	}

	private void addRuleComponent(TranslationRuleInstance<?> trule)
	{
		RuleComponent r = new RuleComponent(msg, registry, 
				trule, actionComponentProvider, new CallbackImplementation());
		
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
	
	public void setRemoteAuthnInput(RemotelyAuthenticatedInput remoteAuthnInput)
	{
		this.remoteAuthnInput = remoteAuthnInput;
		this.testProfileButton.setVisible(true);
	}

	public void setCopyMode()
	{
		name.setReadOnly(false);
		String old = name.getValue();
		name.setValue(msg.getMessage("TranslationProfileEditor.nameCopy", old));
	}
	
	private final class CallbackImplementation implements Callback
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
	}
}
