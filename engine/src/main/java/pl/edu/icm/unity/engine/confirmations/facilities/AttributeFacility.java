/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import java.util.Collection;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.confirmations.ConfirmationFacility;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.VerifiableElement;
import pl.edu.icm.unity.confirmations.states.AttribiuteState;
import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.engine.internal.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.AttributeExt;

/**
 * Attribute confirmation facility.
 * 
 * @author P. Piernik
 */
public class AttributeFacility implements ConfirmationFacility
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AttributeFacility.class);
	private AttributesHelper attributesHelper;
	private DBSessionManager db;
	private DBAttributes dbAttributes;
	
	@Autowired
	public AttributeFacility(AttributesHelper attributesHelper, DBSessionManager db,
			DBAttributes dbAttributes)
	{
		this.attributesHelper = attributesHelper;
		this.db = db;
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
		return "Confirms attribute with verifiable value";
	}

	@Override
	public ConfirmationStatus confirm(String state) throws EngineException
	{
		AttribiuteState attrState = new AttribiuteState();
		attrState.setSerializedConfiguration(state);

		SqlSession sql = db.getSqlSession(false);
		try
		{
			Collection<AttributeExt<?>> all = attributesHelper
					.getAllAttributesInternal(sql, Long.parseLong(attrState.getOwner()), false,
							attrState.getGroup(), attrState.getType(),
							false);
			for (AttributeExt<?> a : all)
			{
				VerifiableElement el = (VerifiableElement) a.getValues().get(0);
				el.setVerified(true);
				dbAttributes.addAttribute(Long.parseLong(attrState.getOwner()) , a, true, sql);
				log.trace("Confirm attribute " + a.getName() + " in entity "
						+ attrState.getOwner());
			}
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		// TODO
		return new ConfirmationStatus(true, "SUCCESSFULL CONFIRM ATTRIBUTE " + attrState.getType());
	}
}
