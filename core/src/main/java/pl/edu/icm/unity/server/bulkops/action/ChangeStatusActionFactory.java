/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.bulkops.action;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.form.action.SetEntityStateActionFactory.EntityStateLimited;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Allows for changing entity status.
 * 
 * @author K. Benedyczak
 */
@Component
public class ChangeStatusActionFactory extends AbstractEntityActionFactory
{
	public static final String NAME = "bulkChangeStatus";
	private IdentitiesManagement idsMan;
	
	@Autowired
	public ChangeStatusActionFactory(@Qualifier("insecure") IdentitiesManagement idsMan)
	{
		super(NAME, new ActionParameterDesc[] {
				new ActionParameterDesc(
						"status",
						"EntityAction.changeStatus.paramDesc.status",
						EntityStateLimited.class)
		});
		this.idsMan = idsMan;
	}

	@Override
	public TranslationAction getInstance(String... parameters)
	{
		return new ChangeStatusAction(idsMan, this, parameters);
	}

	public static class ChangeStatusAction extends AbstractEntityAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER,
				ChangeStatusActionFactory.ChangeStatusAction.class);
		private IdentitiesManagement idsMan;
		private EntityState state;
		
		public ChangeStatusAction(IdentitiesManagement idsMan,
				TranslationActionDescription description, String[] params)
		{
			super(description, params);
			setParameters(params);
		}

		@Override
		public void invoke(Entity entity)
		{
			if (state == entity.getState())
				return;
			
			log.info("Changing entity " + entity + " status to " + state);
			try
			{
				idsMan.setEntityStatus(new EntityParam(entity.getId()), state);
			} catch (Exception e)
			{
				log.error("Changing entity status failed", e);
			}
		}
		
		private void setParameters(String[] parameters)
		{
			if (parameters.length != 1)
				throw new IllegalArgumentException("Action requires exactly 1 parameter");
			state = EntityState.valueOf(parameters[0]);
		}
	}
}
