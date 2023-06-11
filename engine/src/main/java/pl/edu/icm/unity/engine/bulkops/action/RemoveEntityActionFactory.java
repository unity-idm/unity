/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops.action;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.identity.Entity;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.bulkops.EntityAction;

/**
 * Allows for removing an entity.
 * 
 * @author K. Benedyczak
 */
@Component
public class RemoveEntityActionFactory extends AbstractEntityActionFactory
{
	public static final String NAME = "removeEntity";
	private EntityManagement idsMan;
	
	@Autowired
	public RemoveEntityActionFactory(@Qualifier("insecure") EntityManagement idsMan)
	{
		super(NAME, new ActionParameterDefinition[] {});
		this.idsMan = idsMan;
	}

	@Override
	public EntityAction getInstance(String... parameters)
	{
		return new RemoveEntityAction(idsMan, getActionType(), parameters);
	}

	public static class RemoveEntityAction extends EntityAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_BULK_OPS,
				RemoveEntityActionFactory.RemoveEntityAction.class);
		private EntityManagement idsMan;
		
		public RemoveEntityAction(EntityManagement idsMan,
				TranslationActionType description, String[] params)
		{
			super(description, params);
			this.idsMan = idsMan;
		}

		@Override
		public void invoke(Entity entity)
		{
			log.info("Removing entity " + entity);
			try
			{
				idsMan.removeEntity(new EntityParam(entity.getId()));
			} catch (Exception e)
			{
				log.error("Removing entity failed", e);
			}
		}
	}
}
