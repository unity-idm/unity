/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.forms;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.base.registration.URLQueryPrefillConfig;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntry;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.ExternalDataParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

@Component
public class URLQueryPrefillCreator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, URLQueryPrefillCreator.class);
	private final ExternalDataParser externalDataParser;

	URLQueryPrefillCreator(ExternalDataParser externalDataParser)
	{
		this.externalDataParser = externalDataParser;
	}

	public PrefilledSet create(BaseForm form, Map<String, List<String>> parameters)
	{
		return new PrefilledSet(getPrefilledIdentities(form.getIdentityParams(), parameters), null,
				getPrefilledAttributes(form.getAttributeParams(), parameters), null);
	}

	private Map<Integer, PrefilledEntry<Attribute>> getPrefilledAttributes(
			List<AttributeRegistrationParam> attributeParams, Map<String, List<String>> parameters)
	{
		Map<Integer, PrefilledEntry<Attribute>> prefilled = new HashMap<>();
		for (int i = 0; i < attributeParams.size(); i++)
		{
			AttributeRegistrationParam attributeParam = attributeParams.get(i);
			URLQueryPrefillConfig urlQueryPrefill = attributeParam.getUrlQueryPrefill();
			if (urlQueryPrefill == null)
				continue;
			String value = parameters.getOrDefault(urlQueryPrefill.paramName, emptyList()).stream().findFirst().orElse(null);
			if (value == null)
				continue;
		
			Attribute prefilledAttribute;
			try
			{
				prefilledAttribute = externalDataParser.parseAsAttribute(
						attributeParam.getAttributeType(), 
						attributeParam.getGroup(), Lists.newArrayList(value));
			} catch (IllegalAttributeValueException e)
			{
				log.warn("Can't parse query parameter as pre-filled form attribute", e);
				continue;
			}
			prefilled.put(i, new PrefilledEntry<>(prefilledAttribute, urlQueryPrefill.mode));
		}
		return prefilled;
	}

	private Map<Integer, PrefilledEntry<IdentityParam>> getPrefilledIdentities(
			List<IdentityRegistrationParam> identityParams, Map<String, List<String>> parameters)
	{
		Map<Integer, PrefilledEntry<IdentityParam>> prefilled = new HashMap<>();
		for (int i = 0; i < identityParams.size(); i++)
		{
			IdentityRegistrationParam param = identityParams.get(i);
			URLQueryPrefillConfig urlQueryPrefill = param.getUrlQueryPrefill();
			if (urlQueryPrefill == null)
				continue;
			String value = parameters.getOrDefault(urlQueryPrefill.paramName, emptyList()).stream().findFirst().orElse(null);
			if (value == null)
				continue;
		
			IdentityParam prefilledEntry;
			try
			{
				prefilledEntry = externalDataParser.parseAsIdentity(param.getIdentityType(), value);
			} catch (IllegalIdentityValueException e)
			{
				log.warn("Can't parse query parameter as pre-filled form identity", e);
				continue;
			}
			prefilled.put(i, new PrefilledEntry<>(prefilledEntry, urlQueryPrefill.mode));
		}
		return prefilled;
	}
}
