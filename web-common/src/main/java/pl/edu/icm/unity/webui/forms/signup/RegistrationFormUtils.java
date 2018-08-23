/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.signup;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import com.google.common.base.Functions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
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
		
		Map<String, ConsolidatedAttributeParam> consolidatedAttrParams = getConsolidatedAttributes(form, invitation, ctx);
		for (String attributeName : consolidatedAttrParams.keySet())
		{
			ConsolidatedAttributeParam consolidated = consolidatedAttrParams.get(attributeName);
			if (consolidated.formParam == null
					|| consolidated.formParam.isOptional())
				continue;
			Set<String> attrValues = Sets.newHashSet();
			if (consolidated.invitationParam != null)
			{
				List<String> values = consolidated.invitationParam.getEntry().getValues();
				if (values != null)
					attrValues.addAll(values); 
			}
			if (attrValues.isEmpty() && consolidated.remoteParam != null)
			{
				List<String> values = consolidated.remoteParam.getValues();
				if (values != null)
					attrValues.addAll(values);
			}
			if (attrValues.isEmpty())
				return false;
		}
		
		return true;
	}
	
	private static Map<String, ConsolidatedAttributeParam> getConsolidatedAttributes(RegistrationForm form,
			InvitationWithCode invitation, RemotelyAuthenticatedContext ctx)
	{
		Map<String, ConsolidatedAttributeParam> consolidatedAttrParams = Maps.newHashMap();
		Map<String, AttributeRegistrationParam> formAttributesMap = form.getAttributeParams().stream()
				.collect(Collectors.toMap(
						AttributeRegistrationParam::getAttributeType, 
						Functions.identity()));
		formAttributesMap.forEach((type, param) -> {
				ConsolidatedAttributeParam consolidatedParam = getOrCreate(consolidatedAttrParams, type, ConsolidatedAttributeParam::new);
				consolidatedParam.formParam = param;
		});
		Map<String, PrefilledEntry<Attribute>> invitationAttributesMap = invitation.getAttributes().values().stream()
				.collect(Collectors.toMap(
						entry -> entry.getEntry().getName(), 
						Functions.identity()));
		invitationAttributesMap.forEach((type, param) -> {
				ConsolidatedAttributeParam consolidatedParam = getOrCreate(consolidatedAttrParams, type, ConsolidatedAttributeParam::new);
				consolidatedParam.invitationParam = param;
		});
		Map<String, Attribute> remoteAttributesMap = ctx.getAttributes().stream()
				.collect(Collectors.toMap(
						attr -> attr.getName(), 
						Functions.identity()));
		remoteAttributesMap.forEach((type, param) -> {
				ConsolidatedAttributeParam consolidatedParam = getOrCreate(consolidatedAttrParams, type, ConsolidatedAttributeParam::new);
				consolidatedParam.remoteParam = param;
		});
		return consolidatedAttrParams;
	}

	private static Map<String, ConsolidatedIdentityParam> getConsolidatedIdentities(RegistrationForm form,
			InvitationWithCode invitation, RemotelyAuthenticatedContext ctx)
	{
		Map<String, ConsolidatedIdentityParam> consolidatedIdentityParams = Maps.newHashMap();
		Map<String, IdentityRegistrationParam> formIdentityMap = form.getIdentityParams().stream()
				.collect(Collectors.toMap(
						IdentityRegistrationParam::getIdentityType, 
						Functions.identity()));
		formIdentityMap.forEach((type, param) -> {
				ConsolidatedIdentityParam consolidatedParam = getOrCreate(consolidatedIdentityParams, type, ConsolidatedIdentityParam::new);
				consolidatedParam.formParam = param;
		});
		
		Map<String, PrefilledEntry<IdentityParam>> invitationIdentityMap = invitation.getIdentities().values().stream()
				.collect(Collectors.toMap(
						entry -> entry.getEntry().getTypeId(), 
						Functions.identity()));
		invitationIdentityMap.forEach((type, param) -> {
			ConsolidatedIdentityParam consolidatedParam = getOrCreate(consolidatedIdentityParams, type, ConsolidatedIdentityParam::new);
			consolidatedParam.invitationParam = param;
		});
		
		Map<String, IdentityTaV> remoteIdentityMap = ctx.getIdentities().stream()
				.collect(Collectors.toMap(
						IdentityTaV::getTypeId, 
						Functions.identity()));
		remoteIdentityMap.forEach((type, param) -> {
			ConsolidatedIdentityParam consolidatedParam = getOrCreate(consolidatedIdentityParams, type, ConsolidatedIdentityParam::new);
			consolidatedParam.remoteParam = param;
		});
		return consolidatedIdentityParams;
	}

	private static <T> T getOrCreate(Map<String, T> consolidatedParams, String type, Supplier<T> ctor)
	{
		T consolidatedParam = consolidatedParams.get(type);
		if (consolidatedParam == null)
		{
			consolidatedParam = ctor.get();
			consolidatedParams.put(type, consolidatedParam);
		}
		return consolidatedParam;
	}


	static class ConsolidatedAttributeParam
	{
		public Attribute remoteParam;
		public PrefilledEntry<Attribute> invitationParam;
		public AttributeRegistrationParam formParam;
		
	}
	
	static class ConsolidatedIdentityParam
	{
		IdentityRegistrationParam formParam;
		PrefilledEntry<IdentityParam> invitationParam;
		IdentityTaV remoteParam;
	}
	
	private  RegistrationFormUtils() {}
}
