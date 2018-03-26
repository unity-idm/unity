/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops.action;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.bulkops.EntityAction;
import pl.edu.icm.unity.engine.translation.form.action.SetEntityStateActionFactory.EntityStateLimited;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Allows for changing entity status.
 * 
 * @author K. Benedyczak
 */
@Component
public class ChangeStatusActionFactory extends AbstractEntityActionFactory
{
	public static final String NAME = "changeStatus";
	private EntityManagement idsMan;
	
	@Autowired
	public ChangeStatusActionFactory(@Qualifier("insecure") EntityManagement idsMan)
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition(
						"status",
						"EntityAction.changeStatus.paramDesc.status",
						EntityStateLimited.class, true)
		});
		this.idsMan = idsMan;
	}

	@Override
	public EntityAction getInstance(String... parameters)
	{
		return new ChangeStatusAction(idsMan, getActionType(), parameters);
	}

	public static class ChangeStatusAction extends EntityAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER,
				ChangeStatusActionFactory.ChangeStatusAction.class);
		private EntityManagement idsMan;
		private EntityState state;
		
		public ChangeStatusAction(EntityManagement idsMan,
				TranslationActionType description, String[] params)
		{
			super(description, params);
			this.idsMan = idsMan;
			setParameters(params);
		}

		@Override
		public void invoke(Entity entity)
		{
			if (state == entity.getState())
				return;
			
			log.info("Changing entity {} status to {}", entity.getId(), state);
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
			state = EntityState.valueOf(parameters[0]);
		}
	}
}
