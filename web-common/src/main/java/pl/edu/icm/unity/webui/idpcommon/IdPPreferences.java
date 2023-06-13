/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;


/**
 * User's preferences for an IdP endpoints base class. This class is not UI related, can be moved to core module
 * in future. 
 * @author K. Benedyczak
 */
public abstract class IdPPreferences
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, IdPPreferences.class);
	protected final ObjectMapper mapper = Constants.MAPPER;

	public ObjectNode getSerializedConfiguration() throws InternalException
	{
		ObjectNode main = mapper.createObjectNode();
		serializeAll(main);
		return main;
	}

	protected abstract void serializeAll(ObjectNode main);
	
	public void setSerializedConfiguration(ObjectNode json) throws InternalException
	{
		try
		{
			deserializeAll(json);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}

	protected abstract void deserializeAll(ObjectNode main);
	
	public static void initPreferencesGeneric(PreferencesManagement preferencesMan, IdPPreferences toInit, 
			String id) 
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		EntityParam entity = new EntityParam(ae.getEntityId());
		initPreferencesGeneric(preferencesMan, toInit, id, entity);
	}

	public static void initPreferencesGeneric(PreferencesManagement preferencesMan, IdPPreferences toInit, 
			String id, EntityParam entity) 
	{
		try
		{
			String raw = preferencesMan.getPreference(entity, id);
			if (raw != null)
				toInit.setSerializedConfiguration(JsonUtil.parse(raw));
		} catch (Exception e)
		{
			log.warn("It was impossible to establish preferences for " + entity + " will use defaults", e);
		}
	}
	
	public static void savePreferencesGeneric(PreferencesManagement preferencesMan, 
			IdPPreferences preferences, String id) 
			throws EngineException
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		EntityParam entity = new EntityParam(ae.getEntityId());
		preferencesMan.setPreference(entity, id, JsonUtil.serialize(preferences.getSerializedConfiguration()));
	}
}
