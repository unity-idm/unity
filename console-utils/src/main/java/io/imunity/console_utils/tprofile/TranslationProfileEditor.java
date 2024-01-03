/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console_utils.tprofile;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.DROP_LAYOUT;
import static io.imunity.vaadin.elements.CssClassNames.POINTER;

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
	private Icon addRule;

	private RemotelyAuthenticatedInput remoteAuthnInput;
	private StartStopButton testProfileButton;
	private final ActionParameterComponentProvider actionComponentProvider;
	private Binder<TranslationProfile> binder;
	private boolean readOnlyMode;
	private EditorContext editorContext = EditorContext.EDITOR;
	private final HtmlTooltipFactory htmlTooltipFactory;
	private final Set<String> usedNames;

	public TranslationProfileEditor(MessageSource msg,
			TypesRegistryBase<? extends TranslationActionFactory<?>> registry, ProfileType type,
			ActionParameterComponentProvider actionComponentProvider, NotificationPresenter notificationPresenter,
			HtmlTooltipFactory htmlTooltipFactory, Set<String> usedNames)
	{
		this.msg = msg;
		this.registry = registry;
		this.type = type;
		this.actionComponentProvider = actionComponentProvider;
		this.rules = new ArrayList<>();
		this.notificationPresenter = notificationPresenter;
		this.htmlTooltipFactory = htmlTooltipFactory;
		this.usedNames = usedNames;
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
		int nvalidr = 0;
		for (RuleComponent cr : rules)
		{
			if (!cr.validateRule())
				nvalidr++;
		}

		if (!binder.isValid() || nvalidr != 0)
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
		rulesHeader.addClassName(CssClassNames.MARGIN_VERTICAL.getName());
		addRule = VaadinIcon.PLUS_CIRCLE_O.create();
		addRule.setTooltipText(msg.getMessage("TranslationProfileEditor.newRule"));
		addRule.addClickListener(event -> addRuleComponent(null));
		addRule.addClassName(POINTER.getName());

		testProfileButton = new StartStopButton();
		testProfileButton.setVisible(false);
		testProfileButton.setTooltipText(msg.getMessage("TranslationProfileEditor.testProfile"));
		testProfileButton.addClickListener(e -> testRules(), e -> clearTestResults());

		rulesHeader.add(new NativeLabel(msg.getMessage("TranslationProfileEditor.rules")), addRule, testProfileButton);
		rulesHeader.setAlignItems(Alignment.CENTER);


		FormLayout main = new FormLayout();
		main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		main.addFormItem(name, msg.getMessage("TranslationProfileEditor.name"));
		main.addFormItem(description, msg.getMessage("ServiceEditorBase.description"));
		main.setSizeFull();

		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(false);
		wrapper.setPadding(false);
		wrapper.setSpacing(false);
		Span span = new Span();
		span.setHeight(2, Unit.EM);
		wrapper.add(main, span, rulesHeader, rulesLayout);

		binder = new Binder<>(TranslationProfile.class);
		binder.forField(name)
				.asRequired(msg.getMessage("fieldRequired"))
				.withValidator((v, c) ->
				{
					if (!name.isReadOnly() && usedNames.contains(v))
						return ValidationResult.error(msg.getMessage("TranslationProfileEditor.nameError", v));
					return ValidationResult.ok();

				})
				.bind("name");
		binder.bind(description, "description");
		binder.setBean(new TranslationProfile(
				getDefaultName(usedNames), null, type,
				new ArrayList<>()));
		setSpacing(false);
		setMargin(false);
		setPadding(false);
		add(wrapper);
		refreshRules();
	}

	private String getDefaultName(Set<String> usedNames)
	{
		String proposal = msg.getMessage("TranslationProfileEditor.defaultName");
		int i = 1;
		while (usedNames.contains(proposal))
		{
			proposal = msg.getMessage("TranslationProfileEditor.defaultName") + " " + i++;
		}

		return proposal;

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
		r.setEditorContext(editorContext);

		refreshRules();
	}

	private RuleComponent addRuleComponentAt(TranslationRule trule, int index)
	{
		RuleComponent r = new RuleComponent(msg, registry,
				trule, actionComponentProvider, type, new CallbackImplementation(), notificationPresenter, htmlTooltipFactory);
		r.setEditorContext(editorContext);
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
		drop.addClassName(DROP_LAYOUT.getName());

		DropTarget<HorizontalLayout> dropTarget = DropTarget.create(drop);
		dropTarget.setDropEffect(DropEffect.MOVE);
		dropTarget.addDropListener(event ->
		{
			Optional<Component> dragSource = event.getDragSourceComponent();
			if (dragSource.isPresent() && dragSource.get() instanceof LinkButton)
			{
				event.getDragData().ifPresent(data ->
				{
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
	}

	public void setContext(EditorContext context)
	{
		if (context.equals(EditorContext.WIZARD))
		{
			description.setWidth(100, Unit.PERCENTAGE);
		}
		for (RuleComponent rule : rules)
		{
			rule.setEditorContext(context);
		}
		editorContext = context;

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

