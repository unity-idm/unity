/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.out;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.out.AbstractOutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.CreationMode;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;

/**
 * Creates new outgoing identities.
 *   
 * @author K. Benedyczak
 */
public class CreateIdentityAction extends AbstractOutputTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, CreateIdentityAction.class);
	private IdentityTypeDefinition idType;
	private String idTypeString;
	private Serializable idValueExpression;
	private IdentityTypesRegistry idTypesReg;
	private CreationMode creationMode;

	public CreateIdentityAction(String[] params, TranslationActionDescription desc,
			IdentityTypesRegistry idTypesReg) throws EngineException
	{
		super(desc, params);
		this.idTypesReg = idTypesReg;
		setParameters(params);
	}

	@Override
	protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
			TranslationResult result) throws EngineException
	{
		String value = MVEL.executeExpression(idValueExpression, mvelCtx).toString();
		String cmpValue = idType == null ? value : idType.getComparableValue(value, null, null);
		for (Identity existing: result.getIdentities())
		{
			if (existing.getTypeId().equals(idTypeString))
			{
				if (idType != null && idType.getComparableValue(
						existing.getValue(), null, null).equals(cmpValue))
				{
					log.trace("Identity already exists, skipping");
					return;
				}
				if (idType == null && cmpValue.equals(existing.getValue()))
				{
					log.trace("Identity already exists, skipping");
					return;
				}
			}
		}
		
		Identity newId = new Identity();
		newId.setValue(value);
		newId.setTypeId(idTypeString);
		result.getIdentities().add(newId);
		if (creationMode == CreationMode.PERSISTENT)
			result.getIdentitiesToPersist().add(newId);
		log.debug("Created a new " + creationMode.toString().toLowerCase() + " identity: " + newId);
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length != 3)
			throw new IllegalArgumentException("Action requires exactly 3 parameters");
		idTypeString = parameters[0];
		idValueExpression = MVEL.compileExpression(parameters[1]);
		creationMode = CreationMode.valueOf(parameters[2]);

		try
		{
			idType = idTypesReg.getByName(idTypeString);
			if (idType.isDynamic() && creationMode == CreationMode.PERSISTENT)
				throw new IllegalArgumentException("The identity type " + parameters[0] + 
						" is dynamic so it can not be persisted");
		} catch (IllegalTypeException e)
		{
			idType = null;
			if (creationMode == CreationMode.PERSISTENT)
				throw new IllegalArgumentException("The identity type " + parameters[0] + 
						" is not a valid Unity identity type and therefore can not be persisted");
		}
	}

}
