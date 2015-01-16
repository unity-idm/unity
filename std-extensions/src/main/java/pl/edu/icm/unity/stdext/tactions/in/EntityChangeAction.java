/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.in;

import java.util.Date;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.in.AbstractInputTranslationAction;
import pl.edu.icm.unity.server.translation.in.EntityChange;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.EntityScheduledOperation;

/**
 * Defines a change of the entity.
 * @author K. Benedyczak
 */
public class EntityChangeAction extends AbstractInputTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, EntityChangeAction.class);
	private Date changeDate;
	private EntityScheduledOperation scheduledOp;
	
	public EntityChangeAction(TranslationActionDescription description, String[] params)
	{
		super(description, params);
		setParameters(params);
	}

	@Override
	protected MappingResult invokeWrapped(RemotelyAuthenticatedInput input, Object mvelCtx,
			String currentProfile) throws EngineException
	{
		MappingResult ret = new MappingResult();
		EntityChange change = new EntityChange(scheduledOp, changeDate);
		log.debug("Entity scheduled operation: " + scheduledOp);
		ret.addEntityChange(change);
		return ret;
	}
	
	private void setParameters(String[] parameters)
	{
		if (parameters.length != 2)
			throw new IllegalArgumentException("Action requires exactly 2 parameters");
		changeDate = new Date(System.currentTimeMillis() + Long.parseLong(parameters[1]) * 24 * 3600 * 1000L);
		scheduledOp = EntityScheduledOperation.valueOf(parameters[0]);
	}
}
