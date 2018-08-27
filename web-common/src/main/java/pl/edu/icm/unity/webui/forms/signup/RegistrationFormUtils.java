/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.signup;

import static pl.edu.icm.unity.engine.api.registration.AutoProcessInvitationUtil.getConsolidatedAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import com.google.common.base.Functions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.registration.AutoProcessInvitationUtil.ConsolidatedAttrKey;
import pl.edu.icm.unity.engine.api.registration.AutoProcessInvitationUtil.ConsolidatedAttributes;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntry;

/**
 * Utility methods used for validation of registration forms. 
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
final class RegistrationFormUtils
{
	static boolean areAllRequiredRetrieved(RegistrationForm form, InvitationWithCode invitation, RemotelyAuthenticatedContext ctx)
	{
		Map<String, ConsolidatedIdentityParam> consolidatedIdentityParams = getConsolidatedIdentities(form, invitation, ctx);
		
		for (String identityType : consolidatedIdentityParams.keySet())
		{
			ConsolidatedIdentityParam consolidated = consolidatedIdentityParams.get(identityType);
			if (consolidated.formParam == null)
				continue;
			
			String identityValue = null;
			if (consolidated.invitationParam != null)
			{
				identityValue = consolidated.invitationParam.getEntry().getValue();
			} 
			if (StringUtils.isEmpty(identityValue) && consolidated.remoteParam != null)
			{
				identityValue = consolidated.remoteParam.getValue();
			}
			if (StringUtils.isEmpty(identityValue))
				return false;
		}
		
		Map<ConsolidatedAttrKey, ConsolidatedAttributes> consolidatedAttrParams = getConsolidatedAttributes(form, invitation, ctx);
		for (ConsolidatedAttrKey key : consolidatedAttrParams.keySet())
		{
			ConsolidatedAttributes consolidated = consolidatedAttrParams.get(key);
			if (consolidated.form == null
					|| consolidated.form.isOptional())
				continue;
			Set<String> attrValues = Sets.newHashSet();
			if (consolidated.invitation != null)
			{
				List<String> values = consolidated.invitation.getEntry().getValues();
				if (values != null)
					attrValues.addAll(values); 
			}
			if (attrValues.isEmpty() && consolidated.context != null)
			{
				List<String> values = consolidated.context.getValues();
				if (values != null)
					attrValues.addAll(values);
			}
			if (attrValues.isEmpty())
				return false;
		}
		
		return true;
	}
	
	private static Map<String, ConsolidatedIdentityParam> getConsolidatedIdentities(RegistrationForm form,
			InvitationWithCode invitation, RemotelyAuthenticatedContext ctx)
	{
		Map<String, ConsolidatedIdentityParam> consolidatedIdentityParams = Maps.newHashMap();
		if (form.getIdentityParams() != null && !form.getIdentityParams().isEmpty())
		{
			Map<String, IdentityRegistrationParam> formIdentityMap = form.getIdentityParams().stream()
					.collect(Collectors.toMap(
							IdentityRegistrationParam::getIdentityType, 
							Functions.identity()));
			formIdentityMap.forEach((type, param) -> {
				ConsolidatedIdentityParam consolidatedParam = getOrCreate(consolidatedIdentityParams, type);
				consolidatedParam.formParam = param;
			});
		}
		
		if (invitation.getIdentities() != null && !invitation.getIdentities().isEmpty())
		{
			Map<String, PrefilledEntry<IdentityParam>> invitationIdentityMap = invitation.getIdentities().values().stream()
					.collect(Collectors.toMap(
							entry -> entry.getEntry().getTypeId(), 
							Functions.identity()));
			invitationIdentityMap.forEach((type, param) -> {
				ConsolidatedIdentityParam consolidatedParam = getOrCreate(consolidatedIdentityParams, type);
				consolidatedParam.invitationParam = param;
			});
		}
		
		if (ctx.getIdentities() != null && !ctx.getIdentities().isEmpty())
		{
			Map<String, IdentityTaV> remoteIdentityMap = ctx.getIdentities().stream()
					.collect(Collectors.toMap(
							IdentityTaV::getTypeId, 
							Functions.identity()));
			remoteIdentityMap.forEach((type, param) -> {
				ConsolidatedIdentityParam consolidatedParam = getOrCreate(consolidatedIdentityParams, type);
				consolidatedParam.remoteParam = param;
			});
		}
		return consolidatedIdentityParams;
	}

	static class ConsolidatedIdentityParam
	{
		IdentityRegistrationParam formParam;
		PrefilledEntry<IdentityParam> invitationParam;
		IdentityTaV remoteParam;
	}
	
	private static ConsolidatedIdentityParam getOrCreate(Map<String, ConsolidatedIdentityParam> consolidatedParams, String key)
	{
		ConsolidatedIdentityParam consolidatedParam = consolidatedParams.get(key);
		if (consolidatedParam == null)
		{
			consolidatedParam = new ConsolidatedIdentityParam();
			consolidatedParams.put(key, consolidatedParam);
		}
		return consolidatedParam;
	}
	
	private  RegistrationFormUtils() {}
}
