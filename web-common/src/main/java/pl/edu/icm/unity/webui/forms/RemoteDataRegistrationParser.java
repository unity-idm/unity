/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;

/**
 * Parses and validates remotely obtained data for inclusion as an input of a given form.
 *  
 * @author K. Benedyczak
 */
class RemoteDataRegistrationParser
{
	static Map<String, IdentityTaV> parseRemoteIdentities(BaseForm form,
			RemotelyAuthenticatedContext remotelyAuthenticated)
	{
		List<IdentityRegistrationParam> idParams = form.getIdentityParams();
		Map<String, IdentityTaV> remoteIdentitiesByType = new HashMap<>();	
		if (idParams != null)
		{
			for (IdentityRegistrationParam idParam: idParams)
			{	
				if (idParam.getRetrievalSettings() == ParameterRetrievalSettings.interactive)
					continue;
				
				Collection<IdentityTaV> identities = remotelyAuthenticated.getIdentities();
				for (IdentityTaV id: identities)
					if (id.getTypeId().equals(idParam.getIdentityType()))
					{
						remoteIdentitiesByType.put(id.getTypeId(), id);
						break;
					}
			}
		}
		return remoteIdentitiesByType;
	}
	
	static void assertMandatoryRemoteIdentitiesArePresent(BaseForm form,
			Map<String, IdentityTaV> remoteIdentitiesByType) throws AuthenticationException
	{
		List<IdentityRegistrationParam> idParams = form.getIdentityParams();
		if (idParams == null)
			return;
		for (IdentityRegistrationParam idParam: idParams)
		{	
			if (idParam.isOptional() || !idParam.getRetrievalSettings().isAutomaticOnly())
				continue;
			if (!remoteIdentitiesByType.containsKey(idParam.getIdentityType()))
				throw new AuthenticationException("This registration form may be used only by " +
						"users who were remotely authenticated first and who have " +
						idParam.getIdentityType() + 
						" identity provided by the remote authentication source.");
		}
	}

	
	static Map<String, Attribute> parseRemoteAttributes(BaseForm form,
			RemotelyAuthenticatedContext remotelyAuthenticated)
	{
		List<AttributeRegistrationParam> aParams = form.getAttributeParams();
		Map<String, Attribute> remoteAttributes = new HashMap<>();
		if (aParams != null)
		{
			for (AttributeRegistrationParam aParam: aParams)
			{
				if (aParam.getRetrievalSettings() == ParameterRetrievalSettings.interactive)
					continue;
				Collection<Attribute> attrs = remotelyAuthenticated.getAttributes();
				for (Attribute a: attrs)
					if (a.getName().equals(aParam.getAttributeType()) && 
							GroupPatternMatcher.matches(a.getGroupPath(), aParam.getGroup()))
					{
						remoteAttributes.put(getAttributeKey(aParam), a);
						break;
					}
			}
		}
		return remoteAttributes;
	}

	static void assertMandatoryRemoteAttributesArePresent(BaseForm form,
			Map<String, Attribute> remoteAttributes) throws AuthenticationException
	{
		List<AttributeRegistrationParam> aParams = form.getAttributeParams();
		if (aParams == null)
			return;
		for (AttributeRegistrationParam aParam: aParams)
		{
			if (aParam.isOptional() || !aParam.getRetrievalSettings().isAutomaticOnly())
				continue;
			if (!remoteAttributes.containsKey(getAttributeKey(aParam)))
				throw new AuthenticationException("This registration form may be used only by " +
						"users who were remotely authenticated first and who have attribute '" +
						aParam.getAttributeType() + "' in group '" + aParam.getGroup() 
						+ "' provided by the remote authentication source.");
		}
	}
	
	static String getAttributeKey(AttributeRegistrationParam aParam)
	{
		return aParam.getGroup() + "//" + aParam.getAttributeType();
	}
}
