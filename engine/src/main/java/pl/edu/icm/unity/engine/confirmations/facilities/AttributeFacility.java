/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.confirmations.ConfirmationFacility;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.AttribiuteState;
import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.VerifiableElement;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;

/**
 * Attribute confirmation facility.
 * 
 * @author P. Piernik
 */
public class AttributeFacility extends IdentityFacility implements ConfirmationFacility
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AttributeFacility.class);
	private DBAttributes dbAttributes;

	@Autowired
	public AttributeFacility(DBSessionManager db, DBAttributes dbAttributes,
			DBIdentities dbIdentities)
	{
		super(db, dbIdentities);
		this.dbAttributes = dbAttributes;
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
	protected ConfirmationStatus confirmElements(String state) throws EngineException
	{
		AttribiuteState attrState = getState(state);
		SqlSession sql = db.getSqlSession(false);
		ConfirmationStatus status;
		sql = db.getSqlSession(true);
		try
		{

			Collection<AttributeExt<?>> allAttrs = dbAttributes.getAllAttributes(
					Long.parseLong(attrState.getOwner()), attrState.getGroup(),
					false, attrState.getType(), sql);

			Collection<Attribute<?>> confirmedList = confirmAttribute(
					getAttributesFromExt(allAttrs), attrState.getType(),
					attrState.getGroup(), attrState.getValue());

			for (Attribute<?> attr : confirmedList)
			{
				dbAttributes.addAttribute(Long.parseLong(attrState.getOwner()),
						attr, true, sql);
			}
			sql.commit();
			boolean confirmed = (confirmedList.size() > 0);
			status = new ConfirmationStatus(confirmed,
					confirmed ? "ConfirmationStatus.successAttribute"
							: "ConfirmationStatus.attributeChanged");

		} finally
		{
			db.releaseSqlSession(sql);
		}
		return status;
	}

	private Collection<Attribute<?>> getAttributesFromExt(Collection<AttributeExt<?>> attrs)
	{
		Collection<Attribute<?>> attributes = new ArrayList<Attribute<?>>();
		if (attrs != null)
		{
			for (Attribute<?> a : attrs)
			{
				attributes.add(a);
			}
		}
		return attributes;
	}

	private AttribiuteState getState(String state)
	{
		AttribiuteState attrState = new AttribiuteState();
		attrState.setSerializedConfiguration(state);
		return attrState;
	}

	@Override
	public void updateSendedRequest(String state) throws EngineException
	{
		AttribiuteState attrState = new AttribiuteState();
		attrState.setSerializedConfiguration(state);
		SqlSession sql = db.getSqlSession(false);
		sql = db.getSqlSession(true);
		try
		{

			Collection<AttributeExt<?>> allAttrs = dbAttributes.getAllAttributes(
					Long.parseLong(attrState.getOwner()), attrState.getGroup(),
					false, attrState.getType(), sql);

			for (Attribute<?> attr : allAttrs)
			{
				for (Object val : attr.getValues())
				{
					updateConfirmationAmount((VerifiableElement) val, attrState.getValue());
				}
				dbAttributes.addAttribute(Long.parseLong(attrState.getOwner()),
						attr, true, sql);
			}
			sql.commit();

		} finally
		{
			db.releaseSqlSession(sql);
		}

	}
}
