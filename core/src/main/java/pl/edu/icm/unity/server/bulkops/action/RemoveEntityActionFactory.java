/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.bulkops.action;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.translation.TranslationAction;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Allows for removing an entity.
 * 
 * @author K. Benedyczak
 */
@Component
public class RemoveEntityActionFactory extends AbstractEntityActionFactory
{
	public static final String NAME = "removeEntity";
	private IdentitiesManagement idsMan;
	
	@Autowired
	public RemoveEntityActionFactory(IdentitiesManagement idsMan)
	{
		super(NAME, new ActionParameterDesc[] {});
		this.idsMan = idsMan;
	}

	@Override
	public TranslationAction getInstance(String... parameters)
	{
		return new RemoveEntityAction(idsMan, this, parameters);
	}

	public static class RemoveEntityAction extends AbstractEntityAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER,
				RemoveEntityActionFactory.RemoveEntityAction.class);
		private IdentitiesManagement idsMan;
		
		public RemoveEntityAction(IdentitiesManagement idsMan,
				TranslationActionDescription description, String[] params)
		{
			super(description, params);
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
