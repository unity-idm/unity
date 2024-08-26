/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials.password;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.elements.DialogWithActionFooter;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionViewer;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorContext;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.PasswordEncodingPoolProvider;
import pl.edu.icm.unity.stdext.credential.pass.SCryptEncoder;
import pl.edu.icm.unity.stdext.credential.pass.ScryptParams;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;


class PasswordCredentialDefinitionEditor implements CredentialDefinitionEditor, CredentialDefinitionViewer
{
	private static final double MS_IN_MONTH = 3600000L*24L*30.41;
	private static final int MAX_MONTHS = 48;
	private final MessageSource msg;
	private final MessageTemplateManagement msgTplMan;
	private final SCryptEncoder scryptEncoder;
	private final NotificationPresenter notificationPresenter;
	private final HtmlTooltipFactory htmlTooltipFactory;
	
	private Checkbox limitMaxAge;
	private IntegerField maxAge;
	private PasswordCredentialResetSettingsEditor resetSettings;
	private Binder<PasswordCredential> binder;

	PasswordCredentialDefinitionEditor(MessageSource msg, MessageTemplateManagement msgTplMan,
			PasswordEncodingPoolProvider poolProvider, NotificationPresenter notificationPresenter, HtmlTooltipFactory htmlTooltipFactory)
	{
		this.msg = msg;
		this.msgTplMan = msgTplMan;
		this.notificationPresenter = notificationPresenter;
		this.scryptEncoder = new SCryptEncoder(poolProvider.pool);
		this.htmlTooltipFactory = htmlTooltipFactory;
	}

	@Override
	public Component getViewer(String credentialDefinitionConfiguration)
	{
		PasswordCredential helper = new PasswordCredential();
		helper.setSerializedConfiguration(JsonUtil.parse(credentialDefinitionConfiguration));

		PasswordCredentialResetSettingsEditor viewer = new PasswordCredentialResetSettingsEditor(msg, msgTplMan,
				helper.getPasswordResetSettings());

		long maxAgeMs = helper.getMaxAge();
		double maxAgeMonths = (maxAgeMs)/MS_IN_MONTH;
		BigDecimal maxAgeMonthsRounded = new BigDecimal(maxAgeMonths, new MathContext(2, RoundingMode.HALF_UP));
		long maxAgeDays = maxAgeMs/(3600000L*24);
		String maxAge;
		if (maxAgeMs == PasswordCredential.MAX_AGE_UNDEF)
			maxAge = msg.getMessage("PasswordDefinitionEditor.maxAgeUnlimited");
		else
			maxAge = msg.getMessage("PasswordDefinitionEditor.maxAgeValue",
					maxAgeMonthsRounded.doubleValue(), maxAgeDays);

		FormLayout form = new FormLayout();
		form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		form.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		form.addFormItem(new Span(String.valueOf(helper.getMinScore())), msg.getMessage("PasswordDefinitionEditor.minScore"));
		form.addFormItem(new Span(String.valueOf(helper.getMinLength())), msg.getMessage("PasswordDefinitionEditor.minLength"));
		form.addFormItem(new Span(String.valueOf(helper.getMinClassesNum())), msg.getMessage("PasswordDefinitionEditor.minClasses"));
		form.addFormItem(new Span(msg.getYesNo(helper.isDenySequences())), msg.getMessage("PasswordDefinitionEditor.denySequencesRo"));
		form.addFormItem(new Span(String.valueOf(helper.getHistorySize())), msg.getMessage("PasswordDefinitionEditor.historySize"));
		form.addFormItem(new Span(maxAge), msg.getMessage("PasswordDefinitionEditor.maxAgeRo"));
		form.addFormItem(new Span(String.valueOf(helper.getScryptParams().getWorkFactor())), msg.getMessage("PasswordDefinitionEditor.workFactor"));
		form.addFormItem(new Span(msg.getYesNo(helper.isAllowLegacy())), msg.getMessage("PasswordDefinitionEditor.allowLegacy"));

		viewer.addViewerToLayout(form);
		
		return form;
	}
	
	@Override
	public Component getEditor(String credentialDefinitionConfiguration)
	{
		binder = new Binder<>(PasswordCredential.class);

		Button testMe = new Button(msg.getMessage("PasswordDefinitionEditor.testMe"));
		testMe.addClickListener(event -> showTestDialog());
		IntegerField minScore = new IntegerField();
		minScore.setMin(0);
		minScore.setMax(25);
		minScore.setStepButtonsVisible(true);
		binder.forField(minScore).asRequired().bind("minScore");

		Span scoreNote = new Span(msg.getMessage("PasswordDefinitionEditor.minScoreNote"));
		scoreNote.setWidth(100, Unit.PERCENTAGE);
		IntegerField minLength = new IntegerField();
		minLength.setMin(1);
		minLength.setMax(30);
		minLength.setStepButtonsVisible(true);
		binder.forField(minLength).asRequired().bind("minLength");

		IntegerField minClasses = new IntegerField();
		minClasses.setMin(1);
		minClasses.setMax(4);
		minClasses.setStepButtonsVisible(true);
		binder.forField(minClasses).asRequired().bind("minClassesNum");

		Checkbox denySequences = new Checkbox(msg.getMessage("PasswordDefinitionEditor.denySequences"));
		binder.forField(denySequences).bind("denySequences");

		IntegerField historySize = new IntegerField();
		historySize.setMin(0);
		historySize.setMax(50);
		historySize.setStepButtonsVisible(true);
		binder.forField(historySize).asRequired().bind("historySize");

		limitMaxAge = new Checkbox(msg.getMessage("PasswordDefinitionEditor.limitMaxAge"));
		limitMaxAge.addValueChangeListener(event -> maxAge.setEnabled(limitMaxAge.getValue()));

		maxAge = new IntegerField();
		maxAge.setMin(1);
		maxAge.setMax(MAX_MONTHS);
		maxAge.setStepButtonsVisible(true);
		binder.forField(maxAge).asRequired()
				.withConverter(Integer::longValue, Long::intValue)
				.bind("maxAge");

		IntegerField workFactor = new IntegerField();
		workFactor.setMin(ScryptParams.MIN_WORK_FACTOR);
		int maxWF = scryptEncoder.getMaxAllowedWorkFactor();
		workFactor.setMax(maxWF);
		workFactor.setStepButtonsVisible(true);
		binder.forField(workFactor).asRequired()
				.withConverter(ScryptParams::new, ScryptParams::getWorkFactor)
				.bind("scryptParams");

		Button testWorkFactor = new Button(msg.getMessage("PasswordDefinitionEditor.testWorkFactor"));
		testWorkFactor.addClickListener(event -> showTestWorkFactorDialog());
		Checkbox allowLegacy = new Checkbox(msg.getMessage("PasswordDefinitionEditor.allowLegacy"));
		binder.forField(allowLegacy).asRequired().bind("allowLegacy");

		FormLayout form = new FormLayout();
		form.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		form.addFormItem(testMe, "");
		form.addFormItem(minScore, msg.getMessage("PasswordDefinitionEditor.minScore"))
				.add(htmlTooltipFactory.get(msg.getMessage("PasswordDefinitionEditor.minScoreDesc")));
		form.addFormItem(scoreNote, "");
		form.addFormItem(minLength, msg.getMessage("PasswordDefinitionEditor.minLength"));
		form.addFormItem(minClasses, msg.getMessage("PasswordDefinitionEditor.minClasses"));
		form.addFormItem(denySequences, "");
		form.addFormItem(historySize, msg.getMessage("PasswordDefinitionEditor.historySize"));
		form.addFormItem(limitMaxAge, "");
		form.addFormItem(maxAge, msg.getMessage("PasswordDefinitionEditor.maxAge"));
		form.addFormItem(workFactor, msg.getMessage("PasswordDefinitionEditor.workFactor"))
				.add(htmlTooltipFactory.get(msg.getMessage("PasswordDefinitionEditor.workFactorDesc")));
		form.addFormItem(testWorkFactor, "");
		form.addFormItem(allowLegacy, "")
				.add(htmlTooltipFactory.get(msg.getMessage("PasswordDefinitionEditor.allowLegacyDesc")));

		PasswordCredential helper = new PasswordCredential();
		if (credentialDefinitionConfiguration != null)
			helper.setSerializedConfiguration(JsonUtil.parse(credentialDefinitionConfiguration));
		else
			helper.setScryptParams(new ScryptParams(Math.min(16, maxWF)));
		initUIState(helper);
		resetSettings = new PasswordCredentialResetSettingsEditor(msg, msgTplMan, helper.getPasswordResetSettings());
		resetSettings.addEditorToLayout(form);
				
		return form;
	}

	private void showTestDialog()
	{
		PasswordCredential cred = getCredentialSave();
		if (cred == null)
			return;
		
		new TestPasswordDialog(msg, cred, notificationPresenter).open();
	}
	
	private void showTestWorkFactorDialog()
	{
		PasswordCredential cred = getCredentialSave();
		if (cred == null)
			return;
		new TestWorkFactorDialog(msg, cred, scryptEncoder).open();
	}
	
	private PasswordCredential getCredentialSave()
	{
		try{
			return getCredential();
		} catch (FormValidationException e)
		{
			notificationPresenter.showError(msg.getMessage("PasswordDefinitionEditor.checkConfig"), "");
			return null;
		}
	}
	
	private PasswordCredential getCredential() throws FormValidationException
	{
		if (binder.validate().hasErrors())
			throw new FormValidationException();
		PasswordCredential cred = binder.getBean();
		if (limitMaxAge.getValue())
		{
			long maxAgeMs = maxAge.getValue();
			maxAgeMs *= (long) MS_IN_MONTH;
			cred.setMaxAge(maxAgeMs);
		}
		else
			cred.setMaxAge(PasswordCredential.MAX_AGE_UNDEF);
		cred.setPasswordResetSettings(resetSettings.getValue());

		return cred;
	}
	
	@Override
	public String getCredentialDefinition() throws IllegalCredentialException
	{
		try
		{
			return JsonUtil.serialize(getCredential().getSerializedConfiguration());
		} catch (FormValidationException e)
		{
			throw new IllegalCredentialException("", e);
		}	
	}

	private void initUIState(PasswordCredential helper)
	{
		long maxAgeMonths = Math.round(helper.getMaxAge() / MS_IN_MONTH);
		if (maxAgeMonths > 40)
			maxAgeMonths = 40;
		if (maxAgeMonths < 1)
			maxAgeMonths = 1;
		if (helper.getMaxAge() != PasswordCredential.MAX_AGE_UNDEF)
		{
			limitMaxAge.setValue(true);
			helper.setMaxAge((int)maxAgeMonths);
		}
		else
		{
			limitMaxAge.setValue(false);
			helper.setMaxAge(24);
		}
		binder.setBean(helper);
	}
	
	private static class TestPasswordDialog extends DialogWithActionFooter
	{
		private final MessageSource msg;
		private final NotificationPresenter notificationPresenter;
		private final PasswordCredential config;

		TestPasswordDialog(MessageSource msg, PasswordCredential config, NotificationPresenter notificationPresenter)
		{
			super(msg::getMessage);
			this.msg = msg;
			this.config = config;
			this.notificationPresenter = notificationPresenter;
			setHeaderTitle(msg.getMessage("PasswordDefinitionEditor.testMe"));
			setActionButton(msg.getMessage("close"), this::close);
			setCancelButtonVisible(false);
			setWidth("35em");
			setHeight("22em");
			add(getContents());
		}

		private Component getContents()
		{
			FormLayout layout = new FormLayout();
			PasswordEditorComponent editor = new PasswordEditorComponent(msg, CredentialEditorContext.EMPTY, config, notificationPresenter);
			editor.disablePasswordRepeat();
			layout.add(editor);
			return layout;
		}
	}
}
