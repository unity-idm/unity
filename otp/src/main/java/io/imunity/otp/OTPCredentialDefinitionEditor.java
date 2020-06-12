/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp;

import static io.imunity.tooltip.TooltipExtension.tooltipForConsole;

import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.risto.stepper.IntStepper;

import com.vaadin.data.Binder;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;

@PrototypeComponent
class OTPCredentialDefinitionEditor implements CredentialDefinitionEditor
{
	private MessageSource msg;
	private Binder<OTPDefinitionBean> binder;
	
	@Autowired
	OTPCredentialDefinitionEditor(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public Component getEditor(String credentialDefinitionConfiguration)
	{
		binder = new Binder<>(OTPDefinitionBean.class);
		
		TextField issuer = new TextField(msg.getMessage("OTPCredentialDefinitionEditor.issuer"));
		tooltipForConsole(issuer, msg.getMessage("OTPCredentialDefinitionEditor.issuer.tip"));
		binder.forField(issuer).asRequired().bind("issuerName");
		
		EnumComboBox<HashFunction> hashAlgorithm = new EnumComboBox<>(
				msg.getMessage("OTPCredentialDefinitionEditor.hashAlgorithm"), 
				msg, "OTPCredentialDefinitionEditor.hashAlgorithm.", HashFunction.class, HashFunction.SHA1);
		tooltipForConsole(hashAlgorithm, msg.getMessage("OTPCredentialDefinitionEditor.hashAlgorithm.tip"));
		binder.forField(hashAlgorithm).asRequired().bind("hashFunction");
		
		IntStepper allowedTimeDrift = new IntStepper(msg.getMessage("OTPCredentialDefinitionEditor.allowedTimeDrift"));
		tooltipForConsole(hashAlgorithm, msg.getMessage("OTPCredentialDefinitionEditor.allowedTimeDrift.tip"));
		allowedTimeDrift.setMinValue(0);
		allowedTimeDrift.setMaxValue(2880);
		binder.forField(allowedTimeDrift).asRequired().bind("allowedTimeDriftSteps");
		
		ComboBox<Integer> codeLength = new ComboBox<>(msg.getMessage("OTPCredentialDefinitionEditor.codeLength"));
		tooltipForConsole(codeLength, msg.getMessage("OTPCredentialDefinitionEditor.codeLength.tip"));
		codeLength.setItems(6, 8);
		codeLength.setEmptySelectionAllowed(false);
		binder.forField(codeLength).asRequired().bind("codeLength");
		
		IntStepper timeStep = new IntStepper(msg.getMessage("OTPCredentialDefinitionEditor.timeStep"));
		tooltipForConsole(timeStep, msg.getMessage("OTPCredentialDefinitionEditor.timeStep.tip"));
		timeStep.setMinValue(5);
		timeStep.setMaxValue(180);
		binder.forField(timeStep).asRequired().bind("timeStepSeconds");
		
		if (credentialDefinitionConfiguration != null)
		{
			OTPCredentialDefinition editedValue = JsonUtil.parse(credentialDefinitionConfiguration, 
				OTPCredentialDefinition.class);
			binder.setBean(OTPDefinitionBean.fromOTPDefinition(editedValue));
		} else
		{
			binder.setBean(new OTPDefinitionBean());
		}
		FormLayout form = new FormLayout(issuer, hashAlgorithm, allowedTimeDrift, codeLength, timeStep);
		form.setSpacing(true);
		form.setMargin(true);
		return form;
	}

	@Override
	public String getCredentialDefinition() throws IllegalCredentialException
	{
		if (binder.validate().hasErrors())
			throw new IllegalCredentialException("", new FormValidationException());	
		OTPDefinitionBean configBean = binder.getBean();
		return JsonUtil.toJsonString(configBean.toOTPDefinition());
	}
	

	public static class OTPDefinitionBean
	{
		private int codeLength = 6;
		private HashFunction hashFunction = HashFunction.SHA1;
		private int timeStepSeconds = 30;
		private String issuerName = "Unity";
		private int allowedTimeDriftSteps = 3;

		private static OTPDefinitionBean fromOTPDefinition(OTPCredentialDefinition src)
		{
			OTPDefinitionBean ret = new OTPDefinitionBean();
			ret.allowedTimeDriftSteps = src.allowedTimeDriftSteps;
			ret.codeLength = src.otpParams.codeLength;
			ret.hashFunction = src.otpParams.hashFunction;
			ret.issuerName = src.issuerName;
			ret.timeStepSeconds = src.otpParams.timeStepSeconds;
			return ret;
		}

		private OTPCredentialDefinition toOTPDefinition()
		{
			return new OTPCredentialDefinition(
					new OTPGenerationParams(codeLength, hashFunction, timeStepSeconds), 
					issuerName, allowedTimeDriftSteps);
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
	}
}
