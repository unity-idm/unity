/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.confirmation;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.confirmation.MobileNumberConfirmationConfiguration;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.confirm.MobileNumberConfirmationTemplateDef;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.confirmation.MobileNumberConfirmationManager;
import pl.edu.icm.unity.engine.api.confirmation.SMSCode;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.utils.CodeGenerator;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.stdext.attr.VerifiableMobileNumberAttributeSyntax;

/**
 * Mobile number confirmation manager
 */
@Component
class MobileNumberConfirmationManagerImpl implements MobileNumberConfirmationManager
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CONFIRMATION, MobileNumberConfirmationManagerImpl.class);
	
	private final NotificationProducer notificationProducer;
	private final MessageSource msg;
	private final AttributeTypeHelper attrTypeHelper;
	private final Cache<String, Integer> confirmationReqCache;
	private final int requestLimit;
	
	@Autowired
	MobileNumberConfirmationManagerImpl(NotificationProducer notificationProducer,
			MessageSource msg, AttributeTypeHelper attrTypeHelper,
			UnityServerConfiguration mainConf)
	{
		this.notificationProducer = notificationProducer;
		this.msg = msg;
		this.attrTypeHelper = attrTypeHelper;

		confirmationReqCache = CacheBuilder.newBuilder()
				.expireAfterWrite(Duration.ofHours(24))
				.build();
		requestLimit = mainConf
				.getIntValue(UnityServerConfiguration.MOBILE_CONFIRMATION_REQUEST_LIMIT);

	}
	
	@Override
	public SMSCode sendConfirmationRequest(MobileNumberConfirmationConfiguration configEntry, String mobileToConfirm,
			ConfirmationInfo relatedConfirmationInfo) throws EngineException
	{

		if (configEntry == null)
				return null;
		
		if (!checkSendingLimit(mobileToConfirm))
			return null;
		
		String code = CodeGenerator.generateNumberCode(configEntry.getCodeLength());

		HashMap<String, String> params = new HashMap<>();
		params.put(MobileNumberConfirmationTemplateDef.CONFIRMATION_CODE, code);

		log.info("Send sms confirmation request to mobile " + mobileToConfirm + " with code = "
				+ code);

		notificationProducer.sendNotification(mobileToConfirm,
				configEntry.getMessageTemplate(), params, msg.getLocaleCode());	
		if (relatedConfirmationInfo != null)
			relatedConfirmationInfo.incRequestSent();
		
		SMSCode ret = new SMSCode(
				System.currentTimeMillis()
						+ (configEntry.getValidityTime() * 60 * 1000),
				code, mobileToConfirm);
		incCachedConfirmations(mobileToConfirm);
		
		return ret;
	}
	
	private void incCachedConfirmations(String mobileToConfirm)
	{
		String key = getCacheKey(mobileToConfirm);
		Integer currentConfirmations = confirmationReqCache.getIfPresent(key);
		confirmationReqCache.put(key, currentConfirmations == null ? 1 : currentConfirmations + 1);
	}

	private String getCacheKey(String mobileToConfirm)
	{
		return mobileToConfirm.toLowerCase(Locale.US);
	}

	@Override
	public Optional<MobileNumberConfirmationConfiguration> getConfirmationConfigurationForAttribute(
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
			return Optional.empty();
		}
	}
	
	private boolean checkSendingLimit(String mobileToConfirm)
	{
		Integer alreadySent = confirmationReqCache.getIfPresent(getCacheKey(mobileToConfirm));
		if (alreadySent != null && alreadySent >= requestLimit)
		{		
			log.warn("Limit of sent confirmation requests to mobile " + mobileToConfirm + 
					" was reached. (Limit=" +requestLimit + "/24H)");
			return false;
		}
		return true;
	}
	
}
