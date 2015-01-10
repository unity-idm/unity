/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.confirmations.ConfirmationFacility;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.AttribiuteState;
import pl.edu.icm.unity.confirmations.states.BaseConfirmationState;
import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.engine.internal.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;

/**
 * Attribute confirmation facility.
 * 
 * @author P. Piernik
 */
public class AttributeFacility extends FacilityBase implements ConfirmationFacility
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AttributeFacility.class);
	private AttributesHelper attributesHelper;
	private DBSessionManager db;
	private DBAttributes dbAttributes;
	private DBIdentities dbIdentities;

	@Autowired
	public AttributeFacility(AttributesHelper attributesHelper, DBSessionManager db,
			DBAttributes dbAttributes, DBIdentities dbIdentities)
	{
		this.attributesHelper = attributesHelper;
		this.db = db;
		this.dbAttributes = dbAttributes;
		this.dbIdentities = dbIdentities;
	}

	@Override
	public String getName()
	{
		return AttribiuteState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms attributes from entity with verifiable values";
	}

	@Override
	public ConfirmationStatus confirm(String state) throws EngineException
	{
		AttribiuteState attrState = new AttribiuteState();
		attrState.setSerializedConfiguration(state);

		SqlSession sql = db.getSqlSession(false);
		// TODO CHECK ENTITY VALID.
		try
		{
			EntityState entityState = dbIdentities.getEntityStatus(
					Long.parseLong(attrState.getOwner()), sql);

		} catch (Exception e)
		{
			return new ConfirmationStatus(false, "ConfirmationStatus.entityRemoved");

		} finally
		{
			db.releaseSqlSession(sql);
		}
		

		ConfirmationStatus status;
		sql = db.getSqlSession(false);
		try
		{
			
			Collection<Attribute<?>> attributes = new ArrayList<Attribute<?>>();
			Collection<AttributeExt<?>> all = attributesHelper.getAllAttributesInternal(sql,
					Long.parseLong(attrState.getOwner()), false, attrState.getGroup(),
					attrState.getType(), false);
			if (all != null)
			{
				for (Attribute<?> a : all)
				{
					attributes.add(a);
				}
			}

			Collection<Attribute<?>> confirmedList = confirmAttribute(attributes,
					attrState.getType(), attrState.getGroup(), attrState.getValue());

			for (Attribute<?> attr : confirmedList)
			{
				dbAttributes.addAttribute(Long.parseLong(attrState.getOwner()), attr, true,
						sql);
			}

			boolean confirmed = (confirmedList.size() > 0);
			status = new ConfirmationStatus(confirmed,
					confirmed ? "ConfirmationStatus.successAttribute"
							: "ConfirmationStatus.attributeChanged");	
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		return status;
	}
}
