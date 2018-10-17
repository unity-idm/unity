/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.confirmation;

import java.util.HashMap;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Results;
import pl.edu.icm.unity.base.msgtemplates.confirm.MobileNumberConfirmationTemplateDef;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.confirmation.MobileNumberConfirmationManager;
import pl.edu.icm.unity.engine.api.confirmation.SMSCode;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.utils.CacheProvider;
import pl.edu.icm.unity.engine.api.utils.CodeGenerator;
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
	private static final String CACHE_ID = "MobileConfirmationCache";
	
	private NotificationProducer notificationProducer;
	private UnityMessageSource msg;
	private AttributeTypeHelper attrTypeHelper;
	private Ehcache confirmationReqCache;
	private int requestLimit;
	
	@Autowired
	public MobileNumberConfirmationManagerImpl(NotificationProducer notificationProducer,
			UnityMessageSource msg, AttributeTypeHelper attrTypeHelper,
			CacheProvider cacheProvider, UnityServerConfiguration mainConf)
	{
		this.notificationProducer = notificationProducer;
		this.msg = msg;
		this.attrTypeHelper = attrTypeHelper;

		CacheConfiguration cacheConfig = new CacheConfiguration(CACHE_ID, 0);
		Searchable searchable = new Searchable();
		searchable.values(true);
		cacheConfig.addSearchable(searchable);
		cacheConfig.setTimeToIdleSeconds(24 * 3600);
		cacheConfig.setTimeToLiveSeconds(24 * 3600);
		cacheConfig.setEternal(false);
		PersistenceConfiguration persistCfg = new PersistenceConfiguration();
		persistCfg.setStrategy("none");
		cacheConfig.persistence(persistCfg);
		confirmationReqCache = cacheProvider.getManager()
				.addCacheIfAbsent(new Cache(cacheConfig));
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

		log.debug("Send sms confirmation request to mobile " + mobileToConfirm + " with code = "
				+ code);

		notificationProducer.sendNotification(mobileToConfirm,
				configEntry.getMessageTemplate(), params, msg.getLocaleCode());	
		if (relatedConfirmationInfo != null)
			relatedConfirmationInfo.incRequestSent();
		
		SMSCode ret = new SMSCode(
				System.currentTimeMillis()
						+ (configEntry.getValidityTime() * 60 * 1000),
				code, mobileToConfirm);
		
		confirmationReqCache.put(new Element(ret, mobileToConfirm));
		
		return ret;
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
		confirmationReqCache.evictExpiredElements();
		Results results = confirmationReqCache.createQuery().includeValues().addCriteria(
				Query.VALUE.ilike(mobileToConfirm)).execute();
		if (results.size() >= requestLimit)
		{		
			log.warn("Limit of sent confirmation requests to mobile " + mobileToConfirm + 
					" was reached. (Limit=" +requestLimit + "/24H)");
			return false;
		}
		return true;
	}
	
}
