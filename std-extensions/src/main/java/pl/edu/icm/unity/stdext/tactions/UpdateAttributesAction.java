/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemoteInformationBase;
import pl.edu.icm.unity.server.authn.remote.RemoteVerificatorUtil;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Assigns selected attributes to a previously mapped identity. Might be configured to only update the values of 
 * existing attributes.
 *   
 * @author K. Benedyczak
 */
public class UpdateAttributesAction extends AbstractTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, UpdateAttributesAction.class);
	private Pattern selection;
	private boolean onlyValues;
	private AttributesManagement attrMan;
		
	public UpdateAttributesAction(String[] parameters, AttributesManagement attrMan)
	{
		setParameters(parameters);
		this.attrMan = attrMan;
	}
	
	@Override
	public String getName()
	{
		return UpdateAttributesActionFactory.NAME;
	}

	@Override
	protected void invokeWrapped(RemotelyAuthenticatedInput input) throws EngineException
	{
		RemoteIdentity ri = input.getPrimaryIdentity();
		if (ri == null)
		{
			log.debug("No identity, skipping");
			return;
		}
		String unityIdentity = ri.getMetadata().get(RemoteInformationBase.UNITY_IDENTITY);
		if (unityIdentity == null)
		{
			log.debug("No mapped identity, skipping");
			return;
		}
		EntityParam entity = new EntityParam(new IdentityTaV(ri.getIdentityType(), unityIdentity));
		
		Set<String> existingANames = new HashSet<>();
		if (onlyValues)
		{
			Collection<AttributeExt<?>> existingAttrs = attrMan.getAllAttributes(entity, 
					false, null, null, false);
			for (AttributeExt<?> a: existingAttrs)
				existingANames.add(a.getGroupPath()+"///" + a.getName());
		}

		List<Attribute<?>> attrs = RemoteVerificatorUtil.extractAttributes(input, attrMan);
		for (Attribute<?> attr: attrs)
		{
			if (!selection.matcher(attr.getName()).matches())
				continue;
			if (onlyValues && !existingANames.contains(attr.getGroupPath()+"///"+attr.getName()))
				continue;
			log.info("Updating attribute " + attr);
			attrMan.setAttribute(entity, attr, true);
		}
	}

	@Override
	public String[] getParameters()
	{
		return new String[] {selection.pattern(), String.valueOf(onlyValues)};
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length != 2)
			throw new IllegalArgumentException("Action requires exactely 2 parameters");
		selection = Pattern.compile(parameters[0]);
		onlyValues = Boolean.getBoolean(parameters[1]);
	}
}
