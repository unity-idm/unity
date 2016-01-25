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
import pl.edu.icm.unity.server.translation.AbstractTranslationProfile;
import pl.edu.icm.unity.server.translation.AbstractTranslationRule;
import pl.edu.icm.unity.server.translation.RuleFactory;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionFactory;
import pl.edu.icm.unity.server.translation.TranslationProfile;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
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
public abstract class TranslationProfileEditor<T extends TranslationAction, R extends AbstractTranslationRule<T>> 
	extends VerticalLayout
{
	protected UnityMessageSource msg;
	protected TypesRegistryBase<? extends TranslationActionFactory<T>> registry;
	protected boolean editMode;
	protected AbstractTextField name;
	protected DescriptionTextArea description;
	protected FormLayout rulesLayout;
	protected List<RuleComponent<T>> rules;
	
	private RemotelyAuthenticatedInput remoteAuthnInput;
	private StartStopButton testProfileButton;
	private RuleFactory<T> ruleFactory;
	private Provider actionComponentProvider;
	
	public TranslationProfileEditor(UnityMessageSource msg,
			TypesRegistryBase<? extends TranslationActionFactory<T>> registry, TranslationProfile<T> toEdit,
			RuleFactory<T> ruleFactory, 
			ActionParameterComponentFactory.Provider actionComponentProvider) throws EngineException
	{
		super();
		this.msg = msg;
		this.registry = registry;
		this.ruleFactory = ruleFactory;
		this.actionComponentProvider = actionComponentProvider;
		this.rules = new ArrayList<RuleComponent<T>>();
		editMode = toEdit != null;
		initUI(toEdit);
	}

	public AbstractTranslationProfile<T, R> getProfile() throws FormValidationException
	{
		int nvalidr= 0;
		for (RuleComponent<T> cr : rules)
		{
			if (!cr.validateRule())
			{
				nvalidr++;
			}
		}	
		name.setValidationVisible(true);
		if (!name.isValid() || nvalidr != 0)
			throw new FormValidationException();
		String n = name.getValue();
		String desc = description.getValue();
		List<R> trules = new ArrayList<>();
		for (RuleComponent<T> cr : rules)
		{
			@SuppressWarnings("unchecked")
			R r = (R) cr.getRule();
			if (r != null)
			{
				trules.add(r);
			}

		}
		AbstractTranslationProfile<T, R> profile = createProfile(n, trules);
		profile.setDescription(desc);
		return profile;
	}
	
	protected void initUI(TranslationProfile<T> toEdit)
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

		if (editMode)
		{
			name.setValue(toEdit.getName());
			name.setReadOnly(true);
			description.setValue(toEdit.getDescription());
			for (AbstractTranslationRule<T> trule : toEdit.getRules())
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
		for (RuleComponent<T> rule : rules)
		{
			rule.test(remoteAuthnInput);
		}
	}

	protected void clearTestResults() 
	{
		for (RuleComponent<T> rule : rules)
		{
			rule.clearTestResult();
		}		
	}

	private void addRuleComponent(AbstractTranslationRule<T> trule)
	{
		RuleComponent<T> r = new RuleComponent<T>(msg, registry, 
				trule, new CallbackImplementation(), ruleFactory, actionComponentProvider);
		
		rules.add(r);
		if (trule == null)
		{			
			r.setFocus();
		}

		refreshRules();
	}

	private int getRulePosition(RuleComponent<T> toCheck)
	{
		return rules.indexOf(toCheck);
	}

	protected void refreshRules()
	{
		rulesLayout.removeAllComponents();
		if (rules.size() == 0)
			return;

		for (RuleComponent<T> r : rules)
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
		for (RuleComponent<T> r : rules)
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
		if (editMode)
		{
			name.setReadOnly(false);
			String old = name.getValue();
			name.setValue(msg.getMessage("TranslationProfileEditor.nameCopy", old));
			name.setReadOnly(true);
		}
	}
	
	public abstract AbstractTranslationProfile<T, R> createProfile(String name, List<R> trules);
	
	private final class CallbackImplementation implements Callback<T>
	{
		@Override
		public boolean moveUp(RuleComponent<T> rule)
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
		public boolean moveDown(RuleComponent<T> rule)
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
		public boolean remove(RuleComponent<T> rule)
		{
			rules.remove(rule);
			refreshRules();
			return true;
		}

		@Override
		public boolean moveTop(RuleComponent<T> rule)
		{
			int position = getRulePosition(rule);
			rules.remove(position);
			rules.add(0, rule);
			refreshRules();
			return true;
		}

		@Override
		public boolean moveBottom(RuleComponent<T> rule)
		{
			int position = getRulePosition(rule);
			rules.remove(position);
			rules.add(rule);
			refreshRules();
			return true;
		}
	}
}
