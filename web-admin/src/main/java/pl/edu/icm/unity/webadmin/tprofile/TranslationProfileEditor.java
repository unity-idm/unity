/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.DragAndDropWrapper.WrapperTransferable;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;
import pl.edu.icm.unity.webadmin.tprofile.RuleComponent.Callback;
import pl.edu.icm.unity.webadmin.tprofile.RuleComponent.DragHtmlLabel;
import pl.edu.icm.unity.webadmin.tprofile.StartStopButton.ClickStartEvent;
import pl.edu.icm.unity.webadmin.tprofile.StartStopButton.ClickStopEvent;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.Styles;

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
	protected TypesRegistryBase<? extends TranslationActionFactory<?>> registry;
	protected AbstractTextField name;
	protected DescriptionTextArea description;
	protected VerticalLayout rulesLayout;
	protected List<RuleComponent> rules;
	
	private RemotelyAuthenticatedInput remoteAuthnInput;
	private StartStopButton testProfileButton;
	private ActionParameterComponentProvider actionComponentProvider;
	
	public TranslationProfileEditor(UnityMessageSource msg,
			TypesRegistryBase<? extends TranslationActionFactory<?>> registry, ProfileType type, 
			ActionParameterComponentProvider actionComponentProvider) throws EngineException
	{
		super();
		this.msg = msg;
		this.registry = registry;
		this.type = type;
		this.actionComponentProvider = actionComponentProvider;
		this.rules = new ArrayList<>();
		initUI();
	}

	public void setValue(TranslationProfile toEdit)
	{
		name.setReadOnly(false);
		name.setValue(toEdit.getName());
		name.setReadOnly(true);
		description.setValue(toEdit.getDescription());
		for (TranslationRule trule : toEdit.getRules())
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
		rulesLayout = new VerticalLayout();
		rulesLayout.setImmediate(true);
		rulesLayout.setSpacing(false);
		rulesLayout.setMargin(false);
		rulesLayout.setHeightUndefined();
		
		
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
		addRule.setIcon(Images.vaadinAdd.getResource());
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
		//wrapper.setMargin(false);
		//wrapper.setSpacing(false);
		
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

	private void addRuleComponent(TranslationRule trule)
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

	protected void refreshRules()
	{
		rulesLayout.removeAllComponents();
		if (rules.size() == 0)
			return;
		rulesLayout.addComponent(getDropElement(0));
		for (RuleComponent r : rules)
		{
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
		
		rules.get(0).setTopVisible(false);
		rules.get(rules.size() - 1).setBottomVisible(false);		
		for (RuleComponent r : rules)
		{
			rulesLayout.addComponent(r);
			rulesLayout.addComponent(getDropElement(rules.indexOf(r)));	
		}	
	}

	private DragAndDropWrapper getDropElement(int pos)
	{
		Label l = new Label(" ");
		DragAndDropWrapper wr = new DragAndDropWrapper(l);
		wr.setDropHandler(new DropHandler()
		{

			@Override
			public AcceptCriterion getAcceptCriterion()
			{
				return AcceptAll.get();
			}

			@Override
			public void drop(DragAndDropEvent event)
			{
				WrapperTransferable t = (WrapperTransferable) event
						.getTransferable();

				DragHtmlLabel source = (DragHtmlLabel) t.getDraggedComponent();
				RuleComponent sourceRule = source.getParentRule();

				rules.remove(sourceRule);
				rules.add(pos, sourceRule);
				refreshRules();
			}
		});
		
		return wr;
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
	}
}
