/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.signup;

import java.util.Collection;
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
		
		Map<ConsolidatedAttrKey, ConsolidatedAttributes> consolidatedAttrParams = getConsolidatedAttributes(form, invitation, ctx.getAttributes());
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
	
	private static Map<ConsolidatedAttrKey, ConsolidatedAttributes> getConsolidatedAttributes(RegistrationForm formToSubmit, 
			InvitationWithCode invitation, Collection<Attribute> currentAttrs)
	{
		Map<ConsolidatedAttrKey, ConsolidatedAttributes> consolidated = Maps.newHashMap();
		
		List<AttributeRegistrationParam> toSubmitAttrs = formToSubmit.getAttributeParams();
		if (toSubmitAttrs != null && !toSubmitAttrs.isEmpty())
		{
			Map<ConsolidatedAttrKey, AttributeRegistrationParam> toSubmitAttrsMap = toSubmitAttrs.stream()
					.collect(Collectors.toMap(
							ConsolidatedAttrKey::new, 
							Functions.identity()));
			toSubmitAttrsMap.forEach((key, value) -> {
				ConsolidatedAttributes consolidatedAttrs = getOrCreate(consolidated, key, ConsolidatedAttributes::new);
				consolidatedAttrs.form = value;
			});
		}
		
		if (invitation.getAttributes() != null && !invitation.getAttributes().isEmpty())
		{
			Collection<PrefilledEntry<Attribute>> invitationAttrs = invitation.getAttributes().values();
			Map<ConsolidatedAttrKey, PrefilledEntry<Attribute>> invitationAttrsMap = invitationAttrs.stream()
					.collect(Collectors.toMap(
							ConsolidatedAttrKey::new, 
							Functions.identity()));
			invitationAttrsMap.forEach((key, value) -> {
				ConsolidatedAttributes consolidatedAttrs = getOrCreate(consolidated, key, ConsolidatedAttributes::new);
				consolidatedAttrs.invitation = value;
			});
		}
		
		if (currentAttrs != null && !currentAttrs.isEmpty())
		{
			Map<ConsolidatedAttrKey, Attribute> currentAttrsMap = currentAttrs.stream()
					.collect(Collectors.toMap(
							ConsolidatedAttrKey::new, 
							Functions.identity()));
			currentAttrsMap.forEach((key, value) -> {
				ConsolidatedAttributes consolidatedAttrs = getOrCreate(consolidated, key, ConsolidatedAttributes::new);
				consolidatedAttrs.context = value;
			});
		}
		return consolidated;
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
				ConsolidatedIdentityParam consolidatedParam = getOrCreate(consolidatedIdentityParams, type, ConsolidatedIdentityParam::new);
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
				ConsolidatedIdentityParam consolidatedParam = getOrCreate(consolidatedIdentityParams, type, ConsolidatedIdentityParam::new);
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
				ConsolidatedIdentityParam consolidatedParam = getOrCreate(consolidatedIdentityParams, type, ConsolidatedIdentityParam::new);
				consolidatedParam.remoteParam = param;
			});
		}
		return consolidatedIdentityParams;
	}
	
	private static <T, Z> T getOrCreate(Map<Z, T> consolidatedParams, Z key, Supplier<T> ctor)
	{
		T consolidatedParam = consolidatedParams.get(key);
		if (consolidatedParam == null)
		{
			consolidatedParam = ctor.get();
			consolidatedParams.put(key, consolidatedParam);
		}
		return consolidatedParam;
	}

	static class ConsolidatedIdentityParam
	{
		IdentityRegistrationParam formParam;
		PrefilledEntry<IdentityParam> invitationParam;
		IdentityTaV remoteParam;
	}
	
	static class ConsolidatedAttrKey
	{
		String group;
		String attributeType;
		ConsolidatedAttrKey(String group, String attributeType)
		{
			this.group = group;
			this.attributeType = attributeType;
		}
		ConsolidatedAttrKey(AttributeRegistrationParam registrationParam)
		{
			this(registrationParam.getGroup(), registrationParam.getAttributeType());
		}
		ConsolidatedAttrKey(PrefilledEntry<Attribute> invitationAttr)
		{
			this(invitationAttr.getEntry());
		}
		ConsolidatedAttrKey(Attribute attr)
		{
			this(attr.getGroupPath(), attr.getName());
		}
		@Override
		public String toString()
		{
			return group + "//" + attributeType;
		}
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((attributeType == null) ? 0 : attributeType.hashCode());
			result = prime * result + ((group == null) ? 0 : group.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ConsolidatedAttrKey other = (ConsolidatedAttrKey) obj;
			if (attributeType == null)
			{
				if (other.attributeType != null)
					return false;
			} else if (!attributeType.equals(other.attributeType))
				return false;
			if (group == null)
			{
				if (other.group != null)
					return false;
			} else if (!group.equals(other.group))
				return false;
			return true;
		}
	}
	
	static class ConsolidatedAttributes
	{
		public AttributeRegistrationParam form;
		public PrefilledEntry<Attribute> invitation;
		public Attribute context;
	}
	
	private  RegistrationFormUtils() {}
}
