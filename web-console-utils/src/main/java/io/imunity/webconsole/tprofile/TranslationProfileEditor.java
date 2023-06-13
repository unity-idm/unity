/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.tprofile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vaadin.data.Binder;
import com.vaadin.shared.ui.dnd.DropEffect;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.dnd.DropTargetExtension;

import io.imunity.webconsole.tprofile.RuleComponent.Callback;
import io.imunity.webconsole.tprofile.StartStopButton.ClickStartEvent;
import io.imunity.webconsole.tprofile.StartStopButton.ClickStopEvent;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.widgets.DescriptionTextField;

/**
 * Generic component to edit or add translation profile of any type
 * 
 * @author P. Piernik
 * 
 */
public class TranslationProfileEditor extends VerticalLayout
{
	protected MessageSource msg;
	protected ProfileType type;
	protected TypesRegistryBase<? extends TranslationActionFactory<?>> registry;
	protected TextField name;
	protected DescriptionTextField description;
	private HorizontalLayout rulesHeader;
	protected VerticalLayout rulesLayout;
	protected List<RuleComponent> rules;
	private Button addRule;
	
	private RemotelyAuthenticatedInput remoteAuthnInput;
	private StartStopButton testProfileButton;
	private ActionParameterComponentProvider actionComponentProvider;
	private Binder<TranslationProfile> binder;
	private boolean readOnlyMode;
	
	public TranslationProfileEditor(MessageSource msg,
			TypesRegistryBase<? extends TranslationActionFactory<?>> registry, ProfileType type, 
			ActionParameterComponentProvider actionComponentProvider)
	{
		this.msg = msg;
		this.registry = registry;
		this.type = type;
		this.actionComponentProvider = actionComponentProvider;
		this.rules = new ArrayList<>();
		initUI();
	}

	public void setValue(TranslationProfile toEdit)
	{
		binder.setBean(toEdit);
		name.setReadOnly(true);
		rules.clear();
		for (TranslationRule trule : toEdit.getRules())
			addRuleComponentAt(trule, rules.size());
		refreshRules();
	}
	
	public TranslationProfile getProfile() throws FormValidationException
	{
		int nvalidr= 0;
		for (RuleComponent cr : rules)
		{
			if (!cr.validateRule())
				nvalidr++;
		}	
		
		if (!binder.isValid()  || nvalidr != 0)
		{
			binder.validate();
			throw new FormValidationException();
		}
		
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
		rulesLayout = new VerticalLayout();
		rulesLayout.setSpacing(false);
		rulesLayout.setMargin(false);
		rulesLayout.setHeightUndefined();
		rulesLayout.setWidth(100, Unit.PERCENTAGE);
		rulesLayout.setStyleName(Styles.vDropLayout.toString());
		
		name = new TextField(msg.getMessage("TranslationProfileEditor.name"));

		description = new DescriptionTextField(msg);

		rulesHeader = new HorizontalLayout();
		rulesHeader.setMargin(false);
		addRule = new Button();
		addRule.setDescription(msg.getMessage("TranslationProfileEditor.newRule"));
		addRule.setIcon(Images.add.getResource());
		addRule.addStyleName(Styles.vButtonLink.toString());
		addRule.addStyleName(Styles.toolbarButton.toString());
		addRule.addClickListener(event -> addRuleComponent(null));
		
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
		rulesHeader.addComponents(t, addRule, testProfileButton);

		FormLayout main = new CompactFormLayout();
		main.addComponents(name, description);
		main.setSizeFull();

		VerticalLayout wrapper = new VerticalLayout();
		wrapper.addComponents(main, rulesHeader, rulesLayout);
			
		binder = new Binder<>(TranslationProfile.class);
		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.bind(description, "description");
		binder.setBean(new TranslationProfile(
				msg.getMessage("TranslationProfileEditor.defaultName"), null, type,
				new ArrayList<TranslationRule>()));
		setSpacing(false);
		setMargin(false);
		addComponents(wrapper);
		refreshRules();
	}

	protected void testRules() 
	{
		for (RuleComponent rule : rules)
			rule.test(remoteAuthnInput);
	}

	protected void clearTestResults() 
	{
		for (RuleComponent rule : rules)
			rule.clearTestResult();
	}

	private void addRuleComponent(TranslationRule trule)
	{
		RuleComponent r = addRuleComponentAt(trule, rules.size());
		if (trule == null)
			r.setFocus();
		refreshRules();
	}

	private RuleComponent addRuleComponentAt(TranslationRule trule, int index)
	{
		RuleComponent r = new RuleComponent(msg, registry, 
				trule, actionComponentProvider, type, new CallbackImplementation());
		rules.add(index, r);
		return r;
	}
	
	protected void refreshRules()
	{
		rulesLayout.removeAllComponents();
		if (rules.size() == 0)
			return;
		rulesLayout.addComponent(getDropElement(0));
		for (RuleComponent r : rules)
		{
			if (rules.size() > 1)
			{
				r.setTopVisible(true);
				r.setBottomVisible(true);
			} else
			{
				r.setTopVisible(false);
				r.setBottomVisible(false);
			}	
		}
		
		rules.get(0).setTopVisible(false);
		rules.get(rules.size() - 1).setBottomVisible(false);		
		for (RuleComponent r : rules)
		{
			rulesLayout.addComponent(r);
			rulesLayout.addComponent(getDropElement(rules.indexOf(r)));	
		}	
	}
	
	public void refresh()
	{
		rules.forEach(r -> r.refresh());
	}

	private HorizontalLayout getDropElement(int pos)
	{
		HorizontalLayout drop = new HorizontalLayout();
		drop.setWidth(100, Unit.PERCENTAGE);
		drop.setHeight(1, Unit.EM);

		DropTargetExtension<HorizontalLayout> dropTarget = new DropTargetExtension<>(drop);
		dropTarget.setDropEffect(DropEffect.MOVE);	
		dropTarget.addDropListener(event -> {
			Optional<AbstractComponent> dragSource = event.getDragSourceComponent();
			if (dragSource.isPresent() && dragSource.get() instanceof Button)
			{
				event.getDragData().ifPresent(data -> {
					RuleComponent sourceRule = (RuleComponent) data;
					rules.remove(sourceRule);
					rules.add(pos, sourceRule);
					refreshRules();
				});
			}

		});
		
		return drop;
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
	
	public void setReadOnlyMode()
	{
		name.setReadOnly(true);
		description.setReadOnly(true);
		addRule.setVisible(false);
		for (RuleComponent rule : rules)
		{
			rule.setReadOnlyMode(true);
		}
		readOnlyMode = true;
	}
	
	public boolean isReadOnlyMode()
	{
		return readOnlyMode;
	}
	
	public void rulesOnlyMode()
	{
		removeAllComponents();
		addComponent(rulesHeader);
		addComponent(rulesLayout);
	}
	
	public void focusFirst()
	{
		addRule.focus();
	}
	
	private final class CallbackImplementation implements Callback
	{
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
			rules.remove(rule);
			rules.add(0, rule);
			refreshRules();
			return true;
		}

		@Override
		public boolean moveBottom(RuleComponent rule)
		{
			rules.remove(rule);
			rules.add(rule);
			refreshRules();
			return true;
		}

		@Override
		public boolean embedProfile(RuleComponent rule, String profileName, ProfileType profileType)
		{
			int indexOf = rules.indexOf(rule);
			
			TranslationProfile profile;
			try
			{
				profile = (profileType == ProfileType.INPUT) ? 
						actionComponentProvider.getInputProfile(profileName) : 
						actionComponentProvider.getOutputProfile(profileName);
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, msg.getMessage("TranslationProfilesComponent.errorReadData"), e);
				return false;
			}
			
			for (TranslationRule trule : profile.getRules())
				addRuleComponentAt(trule, indexOf++);
			rules.remove(rule);
			refreshRules();
			return true;
		}
	}
}
