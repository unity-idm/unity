/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialDefinitionViewer;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

@PrototypeComponent
class OTPCredentialDefinitionViewer implements CredentialDefinitionViewer
{
	private final MessageSource msg;
	
	OTPCredentialDefinitionViewer(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public Component getViewer(String credentialDefinitionConfiguration)
	{
		OTPCredentialDefinition credentialDef = JsonUtil.parse(credentialDefinitionConfiguration,
				OTPCredentialDefinition.class);
		
		Span issuer = new Span(credentialDef.issuerName);

		Span hashAlgorithm = new Span(msg.getMessage("OTPCredentialDefinitionEditor.hashAlgorithm."
				+ credentialDef.otpParams.hashFunction.name()));

		Span allowedTimeDrift = new Span(String.valueOf(credentialDef.allowedTimeDriftSteps));

		Span codeLength = new Span(String.valueOf(credentialDef.otpParams.codeLength));

		Span timeStep = new Span(credentialDef.otpParams.timeStepSeconds + "s");

		FormLayout form = new FormLayout(issuer, hashAlgorithm, allowedTimeDrift, codeLength, timeStep);
		form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		form.addFormItem(issuer, msg.getMessage("OTPCredentialDefinitionEditor.issuer"));
		form.addFormItem(hashAlgorithm, msg.getMessage("OTPCredentialDefinitionEditor.hashAlgorithm"));
		form.addFormItem(allowedTimeDrift, msg.getMessage("OTPCredentialDefinitionEditor.allowedTimeDrift"));
		form.addFormItem(codeLength, msg.getMessage("OTPCredentialDefinitionEditor.codeLength"));
		form.addFormItem(timeStep, msg.getMessage("OTPCredentialDefinitionEditor.timeStep"));
		return form;
	}
}
