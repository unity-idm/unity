/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp.v8;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;

@PrototypeComponent
class OTPCredentialDefinitionViewer implements CredentialDefinitionViewer
{
	private MessageSource msg;
	
	@Autowired
	OTPCredentialDefinitionViewer(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public Component getViewer(String credentialDefinitionConfiguration)
	{
		OTPCredentialDefinition credentialDef = JsonUtil.parse(credentialDefinitionConfiguration, 
				OTPCredentialDefinition.class);
		
		Label issuer = new Label();
		issuer.setCaption(msg.getMessage("OTPCredentialDefinitionEditor.issuer"));
		issuer.setValue(credentialDef.issuerName);
		
		Label hashAlgorithm = new Label();
		hashAlgorithm.setCaption(msg.getMessage("OTPCredentialDefinitionEditor.hashAlgorithm"));
		hashAlgorithm.setValue(msg.getMessage("OTPCredentialDefinitionEditor.hashAlgorithm." 
					+ credentialDef.otpParams.hashFunction.name()));
		
		Label allowedTimeDrift = new Label();
		allowedTimeDrift.setCaption(msg.getMessage("OTPCredentialDefinitionEditor.allowedTimeDrift"));
		allowedTimeDrift.setValue(String.valueOf(credentialDef.allowedTimeDriftSteps));
		
		Label codeLength = new Label();
		codeLength.setCaption(msg.getMessage("OTPCredentialDefinitionEditor.codeLength"));
		codeLength.setValue(String.valueOf(credentialDef.otpParams.codeLength));
		
		Label timeStep = new Label();
		timeStep.setCaption(msg.getMessage("OTPCredentialDefinitionEditor.timeStep"));
		timeStep.setValue(String.valueOf(credentialDef.otpParams.timeStepSeconds) + "s");

		FormLayout form = new FormLayout(issuer, hashAlgorithm, allowedTimeDrift, codeLength, timeStep);
		form.setSpacing(true);
		form.setMargin(true);
		return form;
	}
}
