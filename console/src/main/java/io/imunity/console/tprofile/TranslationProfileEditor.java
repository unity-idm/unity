/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.tprofile;


import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.Styles;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;

public class TranslationProfileEditor extends VerticalLayout
{
	protected MessageSource msg;
	protected NotificationPresenter notificationPresenter;
	protected ProfileType type;
	protected TypesRegistryBase<? extends TranslationActionFactory<?>> registry;
	protected TextField name;
	protected TextField description;
	private HorizontalLayout rulesHeader;
	protected VerticalLayout rulesLayout;
	protected List<RuleComponent> rules;
	private Button addRule;
	
	private RemotelyAuthenticatedInput remoteAuthnInput;
	private StartStopButton testProfileButton;
	private final ActionParameterComponentProvider actionComponentProvider;
	private Binder<TranslationProfile> binder;
	private boolean readOnlyMode;
	
	public TranslationProfileEditor(MessageSource msg,
									TypesRegistryBase<? extends TranslationActionFactory<?>> registry, ProfileType type,
									ActionParameterComponentProvider actionComponentProvider, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.registry = registry;
		this.type = type;
		this.actionComponentProvider = actionComponentProvider;
		this.rules = new ArrayList<>();
		this.notificationPresenter = notificationPresenter;
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
		rulesLayout.setSizeUndefined();
		rulesLayout.setWidthFull();
		rulesLayout.setPadding(false);
		

		name = new TextField();
		name.setWidth(TEXT_FIELD_MEDIUM.value());

		description = new TextField();
		description.setWidth(TEXT_FIELD_BIG.value());

		rulesHeader = new HorizontalLayout();
		rulesHeader.setMargin(false);
		rulesHeader.setPadding(false);
		addRule = new Button();
		addRule.setTooltipText(msg.getMessage("TranslationProfileEditor.newRule"));
		addRule.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
		addRule.addClickListener(event -> addRuleComponent(null));
		addRule.addThemeVariants(ButtonVariant.LUMO_ICON);
		
		testProfileButton = new StartStopButton();
		testProfileButton.setVisible(false);
		testProfileButton.setTooltipText(msg.getMessage("TranslationProfileEditor.testProfile"));
		testProfileButton.addClickListener(e -> testRules(), e -> clearTestResults());

		Span t = new Span(msg.getMessage("TranslationProfileEditor.rules"));
		rulesHeader.add(t, addRule, testProfileButton);

		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addFormItem(name, msg.getMessage("TranslationProfileEditor.name"));
		main.addFormItem(description, msg.getMessage("ServiceEditorBase.description"));
		main.setSizeFull();

		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(false);
		wrapper.setPadding(false);
		wrapper.setSpacing(false);
		wrapper.add(main, rulesHeader, rulesLayout);
			
		binder = new Binder<>(TranslationProfile.class);
		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.bind(description, "description");
		binder.setBean(new TranslationProfile(
				msg.getMessage("TranslationProfileEditor.defaultName"), null, type,
				new ArrayList<>()));
		setSpacing(false);
		setMargin(false);
		setPadding(false);
		add(wrapper);
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
				trule, actionComponentProvider, type, new CallbackImplementation(), notificationPresenter);
		rules.add(index, r);
		return r;
	}
	
	protected void refreshRules()
	{
		rulesLayout.removeAll();
		if (rules.size() == 0)
			return;
		rulesLayout.add(getDropElement(0));
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
			rulesLayout.add(r);
			rulesLayout.add(getDropElement(rules.indexOf(r)));
		}	
	}
	
	public void refresh()
	{
		rules.forEach(RuleComponent::refresh);
	}

	private HorizontalLayout getDropElement(int pos)
	{
		HorizontalLayout drop = new HorizontalLayout();
		drop.setWidthFull();
		drop.setHeight(1, Unit.EM);
		drop.addClassName(Styles.dropLayout.toString());
		
		DropTarget<HorizontalLayout> dropTarget = DropTarget.create(drop);
		dropTarget.setDropEffect(DropEffect.MOVE);
		dropTarget.addDropListener(event -> {
			Optional<Component> dragSource = event.getDragSourceComponent();
			if (dragSource.isPresent() && dragSource.get() instanceof LinkButton)
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
		removeAll();
		add(rulesHeader, rulesLayout);
	}
	
	public void focusFirst()
	{
		addRule.focus();
	}
	
	private final class CallbackImplementation implements RuleComponent.Callback
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
