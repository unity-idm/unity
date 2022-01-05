/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp;

import static com.google.common.base.Strings.isNullOrEmpty;
import static io.imunity.tooltip.TooltipExtension.tooltip;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.data.Binder;
import com.vaadin.data.Validator;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import io.imunity.otp.OTPResetSettings.ConfirmationMode;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.stdext.credential.pass.EmailPasswordResetTemplateDef;
import pl.edu.icm.unity.stdext.credential.pass.MobilePasswordResetTemplateDef;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.file.ImageField;

@PrototypeComponent
class OTPCredentialDefinitionEditor implements CredentialDefinitionEditor
{
	private final int WIDE_FIELD_SIZE_EM = 20;
	private MessageSource msg;
	private Binder<OTPDefinitionBean> binder;
	private MessageTemplateManagement msgTplManagement;
	private IntStepper resetCodeLength;
	private EnumComboBox<ConfirmationMode> confirmationMode;
	private CompatibleTemplatesComboBox resetEmailMsgTemplateCombo;
	private CompatibleTemplatesComboBox resetSMSCodeTemplateCombo;
	private CheckBox enableReset;
	private FileStorageService fileStorageService;
	private ImageAccessService imageAccessService;
	private URIAccessService uriAccessService;
	private UnityServerConfiguration serverConfig;
	
	@Autowired
	OTPCredentialDefinitionEditor(MessageSource msg, MessageTemplateManagement msgTplManagement,
			FileStorageService fileStorageService, ImageAccessService imageAccessService,
			URIAccessService uriAccessService,  UnityServerConfiguration serverConfig)
	{
		this.msg = msg;
		this.msgTplManagement = msgTplManagement;
		this.fileStorageService = fileStorageService;
		this.imageAccessService = imageAccessService;
		this.uriAccessService = uriAccessService;
		this.serverConfig = serverConfig;
	}

	@Override
	public Component getEditor(String credentialDefinitionConfiguration)
	{
		binder = new Binder<>(OTPDefinitionBean.class);
		
		ImageField logo = new ImageField(msg, uriAccessService, serverConfig.getFileSizeLimit());
		logo.setCaption(msg.getMessage("EditOAuthProviderSubView.logo"));
		logo.configureBinding(binder, "logo");
		logo.onlyRemoteSourceMode();
		
		TextField issuer = new TextField(msg.getMessage("OTPCredentialDefinitionEditor.issuer"));
		tooltip(issuer, msg.getMessage("OTPCredentialDefinitionEditor.issuer.tip"));
		binder.forField(issuer).asRequired().bind("issuerName");
		
		ComboBox<Integer> codeLength = new ComboBox<>(msg.getMessage("OTPCredentialDefinitionEditor.codeLength"));
		tooltip(codeLength, msg.getMessage("OTPCredentialDefinitionEditor.codeLength.tip"));
		codeLength.setItems(6, 8);
		codeLength.setEmptySelectionAllowed(false);
		binder.forField(codeLength).asRequired().bind("codeLength");

		IntStepper allowedTimeDrift = new IntStepper(msg.getMessage("OTPCredentialDefinitionEditor.allowedTimeDrift"));
		allowedTimeDrift.setWidth(3, Unit.EM);
		tooltip(allowedTimeDrift, msg.getMessage("OTPCredentialDefinitionEditor.allowedTimeDrift.tip"));
		allowedTimeDrift.setMinValue(0);
		allowedTimeDrift.setMaxValue(2880);
		binder.forField(allowedTimeDrift).asRequired().bind("allowedTimeDriftSteps");
				
		IntStepper timeStep = new IntStepper(msg.getMessage("OTPCredentialDefinitionEditor.timeStep"));
		timeStep.setWidth(3, Unit.EM);
		tooltip(timeStep, msg.getMessage("OTPCredentialDefinitionEditor.timeStep.tip"));
		timeStep.setMinValue(5);
		timeStep.setMaxValue(180);
		binder.forField(timeStep).asRequired().bind("timeStepSeconds");

		EnumComboBox<HashFunction> hashAlgorithm = new EnumComboBox<>(
				msg.getMessage("OTPCredentialDefinitionEditor.hashAlgorithm"), 
				msg, "OTPCredentialDefinitionEditor.hashAlgorithm.", HashFunction.class, HashFunction.SHA1);
		tooltip(hashAlgorithm, msg.getMessage("OTPCredentialDefinitionEditor.hashAlgorithm.tip"));
		binder.forField(hashAlgorithm).asRequired().bind("hashFunction");
		
		
		enableReset = new CheckBox(msg.getMessage("OTPCredentialDefinitionEditor.enableReset"));
		binder.forField(enableReset).bind("enableReset");
		enableReset.addValueChangeListener(e -> updateResetState());
		
		confirmationMode = new EnumComboBox<>(
				msg.getMessage("OTPCredentialDefinitionEditor.resetConfirmationMode"), msg, 
				"OTPResetSettingsConfirmationMode.", ConfirmationMode.class, ConfirmationMode.EMAIL);
		binder.forField(confirmationMode).bind("confirmationMode");
		confirmationMode.addValueChangeListener(e -> changeMsgTemplateState());
		confirmationMode.setWidth(WIDE_FIELD_SIZE_EM, Unit.EM);
		
		resetCodeLength = new IntStepper(msg.getMessage("OTPCredentialDefinitionEditor.resetCodeLength"));
		resetCodeLength.setWidth(3, Unit.EM);
		resetCodeLength.setMinValue(1);
		resetCodeLength.setMaxValue(16);
		binder.forField(resetCodeLength).asRequired().bind("resetCodeLength");
		
		resetEmailMsgTemplateCombo = new CompatibleTemplatesComboBox(
				EmailPasswordResetTemplateDef.NAME, msgTplManagement);
		resetEmailMsgTemplateCombo.setDefaultValue();
		resetEmailMsgTemplateCombo.setCaption(msg.getMessage("OTPCredentialDefinitionEditor.emailResetTemaplate"));
		binder.forField(resetEmailMsgTemplateCombo)
			.asRequired(Validator.from(arg -> !(isNullOrEmpty(arg) && enableReset.getValue() 
						&& confirmationMode.getValue().requiresEmailConfirmation()), 
				msg.getMessage("fieldRequired")))
			.bind("emailSecurityCodeMsgTemplate");
		resetEmailMsgTemplateCombo.setWidth(WIDE_FIELD_SIZE_EM, Unit.EM);

		resetSMSCodeTemplateCombo = new CompatibleTemplatesComboBox(
				MobilePasswordResetTemplateDef.NAME, msgTplManagement);
		resetSMSCodeTemplateCombo.setDefaultValue();
		resetSMSCodeTemplateCombo.setCaption(msg.getMessage("OTPCredentialDefinitionEditor.mobileResetTemaplate"));
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
			binder.setBean(OTPDefinitionBean.fromOTPDefinition(editedValue, imageAccessService));
		} else
		{
			binder.setBean(new OTPDefinitionBean());
		}
		FormLayout form = new FormLayout(logo, issuer, allowedTimeDrift, timeStep, codeLength, hashAlgorithm,  
				enableReset, confirmationMode, resetCodeLength, 
				resetEmailMsgTemplateCombo, resetSMSCodeTemplateCombo);
		form.setSpacing(true);
		form.setMargin(true);
		return form;
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
		private LocalOrRemoteResource logo;
		
		private static OTPDefinitionBean fromOTPDefinition(OTPCredentialDefinition src, ImageAccessService imageAccessService)
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
			ret.logo = src.logoURI.isEmpty()? null : imageAccessService.getEditableImageResourceFromUriWithUnknownTheme(src.logoURI.get()).orElse(null);
			return ret;
		}

		private OTPCredentialDefinition toOTPDefinition(String name, FileStorageService fileStorageService)
		{
			OTPResetSettings resetSettings = new OTPResetSettings(enableReset, resetCodeLength, 
					emailSecurityCodeMsgTemplate, mobileSecurityCodeMsgTemplate, confirmationMode);
			return new OTPCredentialDefinition(
					new OTPGenerationParams(codeLength, hashFunction, timeStepSeconds), 
					issuerName, allowedTimeDriftSteps, resetSettings, Optional.ofNullable(logo.getRemote()));
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

		public LocalOrRemoteResource getLogo()
		{
			return logo;
		}

		public void setLogo(LocalOrRemoteResource logo)
		{
			this.logo = logo;
		}
	}
}
