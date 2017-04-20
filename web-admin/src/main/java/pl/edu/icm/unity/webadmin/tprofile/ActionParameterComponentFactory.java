/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;

/**
 * Responsible for creating {@link ActionParameterComponent}s.
 * Design note: the factory is split in the way that it returns an intermediate Provider object 
 * that in turn returns final components. It was done to allow to create a reusable provider 
 * per user.
 * @author K. Benedyczak
 */
@Component
public class ActionParameterComponentFactory
{
	@Autowired
	private UnityMessageSource msg;
	@Autowired
	private AttributesManagement attrsMan;
	@Autowired
	private IdentitiesManagement idMan;
	@Autowired
	private AuthenticationManagement authnMan;
	@Autowired
	private GroupsManagement groupsMan;
	
	
	public Provider getComponentProvider() throws EngineException
	{
		return new Provider();
	}
	
	public class Provider
	{
		private List<String> groups;
		private Collection<String> credReqs;
		private Collection<String> idTypes;
		private Collection<AttributeType> atTypes;
		
		private Provider() throws EngineException
		{
			this.atTypes = attrsMan.getAttributeTypes();
			this.groups = new ArrayList<>(groupsMan.getChildGroups("/"));
			Collections.sort(groups);
			Collection<CredentialRequirements> crs = authnMan.getCredentialRequirements();
			credReqs = new TreeSet<String>();
			for (CredentialRequirements cr: crs)
				credReqs.add(cr.getName());
			Collection<IdentityType> idTypesF = idMan.getIdentityTypes();
			idTypes = new TreeSet<String>();
			for (IdentityType it: idTypesF)
				if (!it.getIdentityTypeProvider().isDynamic())
					idTypes.add(it.getIdentityTypeProvider().getId());
		}
		
		public ActionParameterComponent getParameterComponent(ActionParameterDefinition param)
		{
			switch (param.getType())
			{
			case ENUM:
				return new EnumActionParameterComponent(param, msg);
			case UNITY_ATTRIBUTE:
				return new AttributeActionParameterComponent(param, msg, atTypes);
			case UNITY_GROUP:
				return new BaseEnumActionParameterComponent(param, msg, groups);
			case UNITY_CRED_REQ:
				return new BaseEnumActionParameterComponent(param, msg, credReqs);
			case UNITY_ID_TYPE:
				return new BaseEnumActionParameterComponent(param, msg, idTypes);
			case EXPRESSION:
				return new ExpressionActionParameterComponent(param, msg);
			case DAYS:
				return new DaysActionParameterComponent(param, msg);
			case LARGE_TEXT:
				return new TextAreaActionParameterComponent(param, msg);
			case I18N_TEXT:
				return new I18nTextActionParameterComponent(param, msg);
			case TEXT:
				return new DefaultActionParameterComponent(param, msg, false);
			case BOOLEAN:
				return new BooleanActionParameterComponent(param, msg);
			default: 
				return new DefaultActionParameterComponent(param, msg);
			}
		}
	}
}
