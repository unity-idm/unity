/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.ibatis.session.SqlSession;
import org.apache.xml.security.utils.IdResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationFacility;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.AttribiuteState;
import pl.edu.icm.unity.confirmations.states.IdentityConfirmationState;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.ConfirmationData;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Identity confirmation facility.
 * 
 * @author P. Piernik
 */
@Component
public class IdentityFacility extends FacilityBase implements ConfirmationFacility
{
	private DBSessionManager db;
	private DBIdentities dbIdentities;
	private IdentityTypesRegistry identityTypesRegistry;

	@Autowired
	protected IdentityFacility(DBSessionManager db, DBIdentities dbIdentities,
			IdentityTypesRegistry identityTypesRegistry)
	{
		this.db = db;
		this.dbIdentities = dbIdentities;
		this.identityTypesRegistry = identityTypesRegistry;
	}

	@Override
	public String getName()
	{
		return IdentityConfirmationState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms verifiable identity";
	}

	@Override
	public ConfirmationStatus confirm(String state) throws EngineException
	{
		IdentityConfirmationState idState = new IdentityConfirmationState();
		idState.setSerializedConfiguration(state);

		SqlSession sql = db.getSqlSession(false);
		// TODO CHECK ENTITY VALID.
		try
		{
			EntityState entityState = dbIdentities.getEntityStatus(
					Long.parseLong(idState.getOwner()), sql);

		} catch (Exception e)
		{
			return new ConfirmationStatus(false, "ConfirmationStatus.entityRemoved");

		} finally
		{
			db.releaseSqlSession(sql);
		}
		ConfirmationStatus status;
		sql = db.getSqlSession(true);
		try
		{
			Identity[] ids = dbIdentities.getIdentitiesForEntityNoContext(
					Long.parseLong(idState.getOwner()), sql);
			Collection<IdentityParam> confirmedList = confirmIdentity(
					identityTypesRegistry,
					new ArrayList<IdentityParam>(Arrays.asList(ids)),
					idState.getType(), idState.getValue());
			for (IdentityParam id : confirmedList)
			{
				dbIdentities.updateIdentityConfirmationData(id,
						id.getConfirmationData(), sql);
			}
			sql.commit();
			boolean confirmed = (confirmedList.size() > 0);
			status = new  ConfirmationStatus(confirmed,
					confirmed ? "ConfirmationStatus.successIdentity"
							: "ConfirmationStatus.identityChanged");
		} finally
		{
			db.releaseSqlSession(sql);
		}
		return status;
	}

}
