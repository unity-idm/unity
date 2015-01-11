/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import java.util.Collection;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationFacility;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.AttribiuteFromRegState;
import pl.edu.icm.unity.confirmations.states.BaseConfirmationState;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.engine.registrations.InternalRegistrationManagment;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.VerifiableElement;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

/**
 * Attribute from registration request confirmation facility.
 * 
 * @author P. Piernik
 * 
 */
@Component
public class AttributeFromRegistrationRequestFacility extends FacilityBase implements
		ConfirmationFacility
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			AttributeFromRegistrationRequestFacility.class);
	public static final String NAME = "registrationRequestVerificator";
	private final String autoAcceptComment = "System - after confirmation";

	protected RegistrationRequestDB requestDB;
	protected DBSessionManager db;
	private RegistrationFormDB formsDB;
	protected InternalRegistrationManagment internalRegistrationManagment;

	@Autowired
	public AttributeFromRegistrationRequestFacility(DBSessionManager db,
			RegistrationRequestDB requestDB, RegistrationFormDB formsDB,
			InternalRegistrationManagment internalRegistrationManagment)
	{
		this.db = db;
		this.requestDB = requestDB;
		this.formsDB = formsDB;
		this.internalRegistrationManagment = internalRegistrationManagment;
	}

	@Override
	public String getName()
	{
		return AttribiuteFromRegState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms attributes from registration request with verifiable values";
	}

	private AttribiuteFromRegState getState(String state)
	{
		AttribiuteFromRegState attrState = new AttribiuteFromRegState();
		attrState.setSerializedConfiguration(state);
		return attrState;
	}

	protected ConfirmationStatus confirmElements(RegistrationRequest req, String state)
			throws EngineException
	{
		AttribiuteFromRegState attrState = getState(state);
		Collection<Attribute<?>> confirmedList = confirmAttribute(req.getAttributes(),
				attrState.getType(), attrState.getGroup(), attrState.getValue());
		boolean confirmed = (confirmedList.size() > 0);
		return new ConfirmationStatus(confirmed,
				confirmed ? "ConfirmationStatus.successAttribute"
						: "ConfirmationStatus.attributeChanged");

	}

	@Override
	public ConfirmationStatus confirm(String state) throws EngineException
	{
		BaseConfirmationState baseState = new BaseConfirmationState();
		baseState.setSerializedConfiguration(state);
		String requestId = baseState.getOwner();

		RegistrationRequestState reqState = null;
		try
		{
			reqState = internalRegistrationManagment.getRequest(requestId);

		} catch (EngineException e)
		{
			return new ConfirmationStatus(false, "ConfirmationStatus.requestDeleted");
		}

		ConfirmationStatus status;
		SqlSession sql = db.getSqlSession(true);
		try
		{
			RegistrationRequest req = reqState.getRequest();
			status = confirmElements(req, state);
			requestDB.update(requestId, reqState, sql);
			sql.commit();

			if (status.isSuccess()
					&& reqState.getStatus() == RegistrationRequestStatus.pending
					&& internalRegistrationManagment
							.checkAutoAcceptCondition(req))

			{
				RegistrationForm form = formsDB.get(req.getFormId(), sql);
				AdminComment internalComment = new AdminComment(autoAcceptComment,
						0, false);
				reqState.getAdminComments().add(internalComment);
				internalRegistrationManagment.acceptRequest(form, reqState, null,
						internalComment, sql);
			} else
			{
				requestDB.update(requestId, reqState, sql);
			}
			sql.commit();

		} finally
		{
			db.releaseSqlSession(sql);
		}

		return status;
	}

	@Override
	public void updateSendedRequest(String state) throws EngineException
	{
		AttribiuteFromRegState attrState = getState(state);
		String requestId = attrState.getOwner();
		RegistrationRequestState reqState = internalRegistrationManagment
				.getRequest(requestId);
		for (Attribute<?> attr : reqState.getRequest().getAttributes())
		{
			for (Object val : attr.getValues())
			{
				updateConfirmationAmount((VerifiableElement) val,
						attrState.getValue());
			}
		}
		SqlSession sql = db.getSqlSession(true);
		try
		{
			requestDB.update(requestId, reqState, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}

	}
}
