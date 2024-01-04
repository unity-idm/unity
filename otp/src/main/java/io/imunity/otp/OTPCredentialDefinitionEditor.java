/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Validator;
import io.imunity.otp.OTPResetSettings.ConfirmationMode;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.elements.ImageField;
import io.imunity.vaadin.elements.TooltipFactory;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.message_templates.CompatibleTemplatesComboBox;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.credential.pass.EmailPasswordResetTemplateDef;
import pl.edu.icm.unity.stdext.credential.pass.MobilePasswordResetTemplateDef;
import pl.edu.icm.unity.webui.common.FormValidationException;

import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

@PrototypeComponent
public class OTPCredentialDefinitionEditor implements CredentialDefinitionEditor
{
	private static final int WIDE_FIELD_SIZE_EM = 20;
	private final MessageSource msg;
	private final HtmlTooltipFactory htmlTooltipFactory;
	private final MessageTemplateManagement msgTplManagement;
	private final FileStorageService fileStorageService;
	private Binder<OTPDefinitionBean> binder;
	private IntegerField resetCodeLength;
	private Select<ConfirmationMode> confirmationMode;
	private CompatibleTemplatesComboBox resetEmailMsgTemplateCombo;
	private CompatibleTemplatesComboBox resetSMSCodeTemplateCombo;
	private Checkbox enableReset;

	OTPCredentialDefinitionEditor(MessageSource msg, MessageTemplateManagement msgTplManagement,
			FileStorageService fileStorageService, HtmlTooltipFactory htmlTooltipFactory)
	{
		this.msg = msg;
		this.msgTplManagement = msgTplManagement;
		this.fileStorageService = fileStorageService;
		this.htmlTooltipFactory = htmlTooltipFactory;
	}

	@Override
	public Component getEditor(String credentialDefinitionConfiguration)
	{
		binder = new Binder<>(OTPDefinitionBean.class);
		FormLayout formLayout = new FormLayout();
		formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		formLayout.setClassName(CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		ImageField logo = new ImageField();
		formLayout.addFormItem(logo, msg.getMessage("OTPCredentialDefinitionEditor.logo"))
						.add(htmlTooltipFactory.get(msg.getMessage("OTPCredentialDefinitionEditor.logo.tip")));
		logo.setWidth("30em");
		binder.forField(logo).bind("logo");

		TextField issuer = new TextField();
		formLayout.addFormItem(issuer, msg.getMessage("OTPCredentialDefinitionEditor.issuer"))
				.add(TooltipFactory.get(msg.getMessage("OTPCredentialDefinitionEditor.issuer.tip")));
		issuer.setWidth(TEXT_FIELD_MEDIUM.value());
		binder.forField(issuer).asRequired().bind("issuerName");

		IntegerField allowedTimeDrift = new IntegerField();
		formLayout.addFormItem(allowedTimeDrift, msg.getMessage("OTPCredentialDefinitionEditor.allowedTimeDrift"))
				.add(TooltipFactory.get(msg.getMessage("OTPCredentialDefinitionEditor.allowedTimeDrift.tip")));
		allowedTimeDrift.setMin(0);
		allowedTimeDrift.setMax(2880);
		allowedTimeDrift.setStepButtonsVisible(true);
		binder.forField(allowedTimeDrift).asRequired().bind("allowedTimeDriftSteps");

		IntegerField timeStep = new IntegerField();
		formLayout.addFormItem(timeStep, msg.getMessage("OTPCredentialDefinitionEditor.timeStep"))
				.add(TooltipFactory.get(msg.getMessage("OTPCredentialDefinitionEditor.timeStep.tip")));
		timeStep.setMin(5);
		timeStep.setMax(180);
		timeStep.setStepButtonsVisible(true);
		binder.forField(timeStep).asRequired().bind("timeStepSeconds");

		Select<Integer> codeLength = new Select<>();
		formLayout.addFormItem(codeLength, msg.getMessage("OTPCredentialDefinitionEditor.codeLength"))
				.add(TooltipFactory.get(msg.getMessage("OTPCredentialDefinitionEditor.codeLength.tip")));
		codeLength.setItems(6, 8);
		codeLength.setWidth(TEXT_FIELD_MEDIUM.value());
		binder.forField(codeLength).asRequired().bind("codeLength");

		Select<HashFunction> hashAlgorithm = new Select<>();
		hashAlgorithm.setItems(HashFunction.values());
		hashAlgorithm.setValue(HashFunction.SHA1);
		hashAlgorithm.setItemLabelGenerator(item -> msg.getMessage("OTPCredentialDefinitionEditor.hashAlgorithm." + item));
		formLayout.addFormItem(hashAlgorithm, msg.getMessage("OTPCredentialDefinitionEditor.hashAlgorithm"))
				.add(TooltipFactory.get(msg.getMessage("OTPCredentialDefinitionEditor.hashAlgorithm.tip")));
		hashAlgorithm.setWidth(TEXT_FIELD_MEDIUM.value());
		binder.forField(hashAlgorithm).asRequired().bind("hashFunction");

		enableReset = new Checkbox(msg.getMessage("OTPCredentialDefinitionEditor.enableReset"));
		formLayout.addFormItem(enableReset, "");
		binder.forField(enableReset).bind("enableReset");
		enableReset.addValueChangeListener(e -> updateResetState());
		
		confirmationMode = new Select<>();
		confirmationMode.setItems(ConfirmationMode.values());
		confirmationMode.setValue(ConfirmationMode.EMAIL);
		confirmationMode.setItemLabelGenerator(item -> msg.getMessage("OTPResetSettingsConfirmationMode." + item));
		formLayout.addFormItem(confirmationMode, msg.getMessage("OTPCredentialDefinitionEditor.resetConfirmationMode"));
		binder.forField(confirmationMode).bind("confirmationMode");
		confirmationMode.addValueChangeListener(e -> changeMsgTemplateState());
		confirmationMode.setWidth(WIDE_FIELD_SIZE_EM, Unit.EM);
		
		resetCodeLength = new IntegerField();
		resetCodeLength.setMin(1);
		resetCodeLength.setMax(16);
		resetCodeLength.setStepButtonsVisible(true);
		formLayout.addFormItem(resetCodeLength, msg.getMessage("OTPCredentialDefinitionEditor.resetCodeLength"));
		binder.forField(resetCodeLength).asRequired().bind("resetCodeLength");
		
		resetEmailMsgTemplateCombo = new CompatibleTemplatesComboBox(
				EmailPasswordResetTemplateDef.NAME, msgTplManagement);
		resetEmailMsgTemplateCombo.setDefaultValue();
		formLayout.addFormItem(resetEmailMsgTemplateCombo,
						msg.getMessage("OTPCredentialDefinitionEditor.emailResetTemaplate"))
				.add(TooltipFactory.get(msg.getMessage("OTPCredentialDefinitionEditor.emailResetTemaplate.tooltip")));
		binder.forField(resetEmailMsgTemplateCombo)
			.asRequired(Validator.from(arg -> !(isNullOrEmpty(arg) && enableReset.getValue()
						&& confirmationMode.getValue().requiresEmailConfirmation()), 
				msg.getMessage("fieldRequired")))
			.bind("emailSecurityCodeMsgTemplate");
		resetEmailMsgTemplateCombo.setWidth(WIDE_FIELD_SIZE_EM, Unit.EM);

		resetSMSCodeTemplateCombo = new CompatibleTemplatesComboBox(
				MobilePasswordResetTemplateDef.NAME, msgTplManagement);
		resetSMSCodeTemplateCombo.setDefaultValue();
		formLayout.addFormItem(resetSMSCodeTemplateCombo,
						msg.getMessage("OTPCredentialDefinitionEditor.mobileResetTemaplate"))
				.add(TooltipFactory.get(msg.getMessage("OTPCredentialDefinitionEditor.mobileResetTemaplate.tooltip")));
		binder.forField(resetSMSCodeTemplateCombo)
			.asRequired(Validator.from(arg -> !(isNullOrEmpty(arg) && enableReset.getValue() 
					&& confirmationMode.getValue().requiresMobileConfirmation()), 
			msg.getMessage("fieldRequired")))
			.bind("mobileSecurityCodeMsgTemplate");
		resetSMSCodeTemplateCombo.setWidth(WIDE_FIELD_SIZE_EM, Unit.EM);
		
		updateResetState();
		
		if (credentialDefinitionConfiguration != null)
		{
			OTPCredentialDefinition editedValue = JsonUtil.parse(credentialDefinitionConfiguration,
				OTPCredentialDefinition.class);
			binder.setBean(OTPDefinitionBean.fromOTPDefinition(editedValue));
		} else
		{
			binder.setBean(new OTPDefinitionBean());
		}
		return formLayout;
	}

	private void updateResetState()
	{
		boolean enabled = enableReset.getValue();
		resetCodeLength.setEnabled(enabled);
		confirmationMode.setEnabled(enabled);
		changeMsgTemplateState();
	}

	private void changeMsgTemplateState()
	{
		boolean enabled = enableReset.getValue();
		ConfirmationMode mode = confirmationMode.getValue();
		resetEmailMsgTemplateCombo.setEnabled(enabled && mode.isEmail());
		resetSMSCodeTemplateCombo.setEnabled(enabled && mode.isMobile());
	}

	
	@Override
	public String getCredentialDefinition() throws IllegalCredentialException
	{
		if (binder.validate().hasErrors())
			throw new IllegalCredentialException("", new FormValidationException());
		OTPDefinitionBean configBean = binder.getBean();
		return JsonUtil.toJsonString(configBean.toOTPDefinition("", fileStorageService));
	}
	

	public static class OTPDefinitionBean
	{
		private int codeLength = 6;
		private HashFunction hashFunction = HashFunction.SHA1;
		private int timeStepSeconds = 30;
		private String issuerName = "Unity";
		private int allowedTimeDriftSteps = 3;

		private boolean enableReset = false;
		private int resetCodeLength = 6;
		private String emailSecurityCodeMsgTemplate;
		private String mobileSecurityCodeMsgTemplate;
		private ConfirmationMode confirmationMode = ConfirmationMode.EMAIL;
		private String logo;
		
		private static OTPDefinitionBean fromOTPDefinition(OTPCredentialDefinition src)
		{
			OTPDefinitionBean ret = new OTPDefinitionBean();
			ret.allowedTimeDriftSteps = src.allowedTimeDriftSteps;
			ret.codeLength = src.otpParams.codeLength;
			ret.hashFunction = src.otpParams.hashFunction;
			ret.issuerName = src.issuerName;
			ret.timeStepSeconds = src.otpParams.timeStepSeconds;
			ret.enableReset = src.resetSettings.enabled;
			ret.resetCodeLength = src.resetSettings.codeLength;
			ret.emailSecurityCodeMsgTemplate = src.resetSettings.emailSecurityCodeMsgTemplate;
			ret.mobileSecurityCodeMsgTemplate = src.resetSettings.mobileSecurityCodeMsgTemplate;
			ret.confirmationMode = src.resetSettings.confirmationMode;
			ret.logo = src.logoURI.orElse(null);
			return ret;
		}

		private OTPCredentialDefinition toOTPDefinition(String name, FileStorageService fileStorageService)
		{
			OTPResetSettings resetSettings = new OTPResetSettings(enableReset, resetCodeLength,
					emailSecurityCodeMsgTemplate, mobileSecurityCodeMsgTemplate, confirmationMode);
			return new OTPCredentialDefinition(
					new OTPGenerationParams(codeLength, hashFunction, timeStepSeconds),
					issuerName, allowedTimeDriftSteps, resetSettings, Optional.ofNullable(logo));
		}
		
		public int getCodeLength()
		{
			return codeLength;
		}
		public void setCodeLength(int codeLength)
		{
			this.codeLength = codeLength;
		}
		public HashFunction getHashFunction()
		{
			return hashFunction;
		}
		public void setHashFunction(HashFunction hashFunction)
		{
			this.hashFunction = hashFunction;
		}
		public int getTimeStepSeconds()
		{
			return timeStepSeconds;
		}
		public void setTimeStepSeconds(int timeStepSeconds)
		{
			this.timeStepSeconds = timeStepSeconds;
		}
		public String getIssuerName()
		{
			return issuerName;
		}
		public void setIssuerName(String issuerName)
		{
			this.issuerName = issuerName;
		}
		public int getAllowedTimeDriftSteps()
		{
			return allowedTimeDriftSteps;
		}
		public void setAllowedTimeDriftSteps(int allowedTimeDriftSteps)
		{
			this.allowedTimeDriftSteps = allowedTimeDriftSteps;
		}

		public boolean isEnableReset()
		{
			return enableReset;
		}

		public void setEnableReset(boolean enableReset)
		{
			this.enableReset = enableReset;
		}

		public int getResetCodeLength()
		{
			return resetCodeLength;
		}

		public void setResetCodeLength(int resetCodeLength)
		{
			this.resetCodeLength = resetCodeLength;
		}

		public String getEmailSecurityCodeMsgTemplate()
		{
			return emailSecurityCodeMsgTemplate;
		}

		public void setEmailSecurityCodeMsgTemplate(String emailSecurityCodeMsgTemplate)
		{
			this.emailSecurityCodeMsgTemplate = emailSecurityCodeMsgTemplate;
		}

		public String getMobileSecurityCodeMsgTemplate()
		{
			return mobileSecurityCodeMsgTemplate;
		}

		public void setMobileSecurityCodeMsgTemplate(String mobileSecurityCodeMsgTemplate)
		{
			this.mobileSecurityCodeMsgTemplate = mobileSecurityCodeMsgTemplate;
		}

		public ConfirmationMode getConfirmationMode()
		{
			return confirmationMode;
		}

		public void setConfirmationMode(ConfirmationMode confirmationMode)
		{
			this.confirmationMode = confirmationMode;
		}

		public String getLogo()
		{
			return logo;
		}

		public void setLogo(String logo)
		{
			this.logo = logo;
		}
	}
}
