/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.confirmation;

import java.util.HashMap;
import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msgtemplates.confirm.MobileNumberConfirmationTemplateDef;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.confirmation.MobileNumberConfirmationManager;
import pl.edu.icm.unity.engine.api.confirmation.SMSCode;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.VerifiableMobileNumberAttributeSyntax;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.MobileNumberConfirmationConfiguration;

/**
 * Mobile number confirmation manager
 * @author P.Piernik
 *
 */
@Component
public class MobileNumberConfirmationManagerImpl implements MobileNumberConfirmationManager
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, MobileNumberConfirmationManagerImpl.class);

	private NotificationProducer notificationProducer;
	private UnityMessageSource msg;
	private AttributeTypeHelper attrTypeHelper;
	
	@Autowired
	public MobileNumberConfirmationManagerImpl(NotificationProducer notificationProducer,
			UnityMessageSource msg, AttributeTypeHelper attrTypeHelper)
	{
		this.notificationProducer = notificationProducer;
		this.msg = msg;
		this.attrTypeHelper = attrTypeHelper;
	}
	
	@Override
	public SMSCode sendConfirmationRequest(MobileNumberConfirmationConfiguration configEntry, String mobileToConfirm,
			ConfirmationInfo relatedConfirmationInfo) throws EngineException
	{

		if (configEntry == null)
				return null;
		
		String code = generateCode(configEntry.getCodeLenght());

		HashMap<String, String> params = new HashMap<>();
		params.put(MobileNumberConfirmationTemplateDef.CONFIRMATION_CODE, code);

		log.debug("Send sms confirmation request to " + mobileToConfirm + " with code = "
				+ code);

		notificationProducer.sendNotification(mobileToConfirm,
				configEntry.getMessageTemplate(), params, msg.getLocaleCode());

		if (relatedConfirmationInfo != null)
			updateConfirmationInfo(relatedConfirmationInfo);
		return new SMSCode(
				System.currentTimeMillis()
						+ (configEntry.getValidityTime() * 60 * 1000),
				code, mobileToConfirm);
	}
	
	private String generateCode(int codeLenght)
	{
		Random rand = new Random();
		String code = "";
		for (int i=0; i < codeLenght ; i++)
			code += String.valueOf(rand.nextInt(10));
		
		return code;
	}

	private void updateConfirmationInfo (ConfirmationInfo toUpdate)
	{
		toUpdate.setConfirmationDate(0);
		toUpdate.setConfirmed(false);
		toUpdate.setSentRequestAmount(toUpdate.getSentRequestAmount() + 1);
	}
	
	@Override
	public MobileNumberConfirmationConfiguration getConfirmationConfigurationForAttribute(
			String attributeName)
	{
		try
		{

			AttributeValueSyntax<?> syntax = attrTypeHelper.getSyntax(
					attrTypeHelper.getTypeForAttributeName(attributeName));
			if (!syntax.getValueSyntaxId()
					.equals(VerifiableMobileNumberAttributeSyntax.ID))
				throw new IllegalArgumentException("Unsupported attribute type: "
						+ attributeName + " for sms confirmation");
			VerifiableMobileNumberAttributeSyntax verifiableMobileSyntax = (VerifiableMobileNumberAttributeSyntax) syntax;
			return verifiableMobileSyntax.getMobileNumberConfirmationConfiguration();

		} catch (Exception e)
		{
			log.debug("Cannot get confirmation configuration for attribute "
					+ attributeName, e);
			return null;
		}
	}
	
	
}
