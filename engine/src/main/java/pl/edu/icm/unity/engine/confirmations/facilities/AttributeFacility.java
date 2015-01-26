/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.AttribiuteConfirmationState;
import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;

/**
 * Attribute confirmation facility.
 * 
 * @author P. Piernik
 */
public class AttributeFacility extends UserFacility<AttribiuteConfirmationState>
{
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
		return AttribiuteConfirmationState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms attributes from entity with verifiable values";
	}

	@Override
	protected ConfirmationStatus confirmElements(AttribiuteConfirmationState attrState) throws EngineException
	{
		ConfirmationStatus status;
		SqlSession sql= db.getSqlSession(true);
		try
		{
			Collection<AttributeExt<?>> allAttrs = dbAttributes.getAllAttributes(
					attrState.getOwnerEntityId(), attrState.getGroup(),
					false, attrState.getType(), sql);

			Collection<Attribute<?>> confirmedList = confirmAttributes(
					getAttributesFromExt(allAttrs), attrState.getType(),
					attrState.getGroup(), attrState.getValue());

			for (Attribute<?> attr : confirmedList)
			{
				dbAttributes.addAttribute(attrState.getOwnerEntityId(), attr, true, sql);
			}
			sql.commit();
			boolean confirmed = (confirmedList.size() > 0);
			status = new ConfirmationStatus(confirmed,
					confirmed ? attrState.getSuccessUrl() : attrState
							.getErrorUrl(),
					confirmed ? "ConfirmationStatus.successAttribute"
							: "ConfirmationStatus.attributeChanged",
					attrState.getType());

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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processAfterSendRequest(String state) throws EngineException
	{
		AttribiuteConfirmationState attrState = new AttribiuteConfirmationState(state);
		SqlSession sql = db.getSqlSession(true);
		try
		{
			Collection<AttributeExt<?>> allAttrs = dbAttributes.getAllAttributes(
					attrState.getOwnerEntityId(), attrState.getGroup(),
					false, attrState.getType(), sql);

			for (Attribute<?> attr : allAttrs)
			{
				if (attr.getAttributeSyntax().isVerifiable())
				{
					for (Object val : attr.getValues())
					{
						updateConfirmationInfo((VerifiableElement) val,
								attrState.getValue());
					}
					dbAttributes.addAttribute(attrState.getOwnerEntityId(), attr, true, sql);
				}
			}
			sql.commit();

		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	protected AttribiuteConfirmationState parseState(String state)
	{
		return new AttribiuteConfirmationState(state);
	}
}
