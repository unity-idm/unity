/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.shared.endpoint.plugins.credentials.sms;

import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditor;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.confirmation.MobileNumberConfirmationManager;
import pl.edu.icm.unity.stdext.credential.sms.SMSVerificator;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer;
import pl.edu.icm.unity.webui.common.credentials.sms.SMSCredentialDefinitionEditor;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;

@Component
public class SMSCredentialEditorFactoryV23 implements CredentialEditorFactory
{
	private MessageSource msg;
	private MessageTemplateManagement msgTplMan;
	private AttributeTypeSupport attrTypeSupport;
	private AttributeSupport attrMan;
	private ConfirmationInfoFormatter formatter;
	private MobileNumberConfirmationManager  mobileConfirmationMan;

	@Autowired
	public SMSCredentialEditorFactoryV23(MessageSource msg,
	                                     AttributeTypeSupport attrTypeSupport, AttributeSupport attrMan,
	                                     MessageTemplateManagement msgTplMan,
	                                     MobileNumberConfirmationManager mobileConfirmationMan,
	                                     ConfirmationInfoFormatter formatter)
	{
		this.msg = msg;
		this.msgTplMan = msgTplMan;
		this.attrTypeSupport = attrTypeSupport;
		this.attrMan = attrMan;
		this.mobileConfirmationMan = mobileConfirmationMan;
		this.formatter = formatter;
	}

	@Override
	public String getSupportedCredentialType()
	{
		return SMSVerificator.NAME;
	}

	@Override
	public CredentialEditor createCredentialEditor()
	{
		return new SMSCredentialEditor(msg, attrTypeSupport, attrMan, mobileConfirmationMan,
				formatter);
	}

	@Override
	public CredentialDefinitionEditor creteCredentialDefinitionEditor()
	{
		return new SMSCredentialDefinitionEditor(msg, msgTplMan);
	}

	@Override
	public CredentialDefinitionViewer creteCredentialDefinitionViewer()
	{
		return new SMSCredentialDefinitionEditor(msg, msgTplMan);
	}
}
