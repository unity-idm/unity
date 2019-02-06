/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.forms.enquiry;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.TranslationCondition;
import pl.edu.icm.unity.engine.bulkops.EntityMVELContextBuilder;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.registration.EnquiryForm;

/**
 * Helper for checking enquiry target condition
 * @author P.Piernik
 *
 */
public class EnquiryTargetCondEvaluator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, EnquiryTargetCondEvaluator.class);
	
	public static boolean evaluateTargetCondition(EnquiryForm form, List<Identity> identities, String entityStatus,
			String credentialReq, Set<String> groups, Collection<AttributeExt> attributes)
	{

		if (!groups.stream().anyMatch(Arrays.asList(form.getTargetGroups())::contains))
		{
			return false;
		}
		
		
		if (form.getTargetCondition() == null || form.getTargetCondition().isEmpty())
		{
			return true;
		}

		Map<String, Object> context = EntityMVELContextBuilder.getContext(identities, entityStatus,
				credentialReq, groups, attributes);

		TranslationCondition condition = new TranslationCondition(form.getTargetCondition());
		try
		{
			return condition.evaluate(context);

		} catch (EngineException e)
		{
			log.error("Cannot evaluate enquriy form target condition" + form.getTargetCondition()
					+ " for entity " + e);
			return false;
		}
	}
}
