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
import pl.edu.icm.unity.confirmations.states.BaseConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationReqAttribiuteConfirmationState;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.engine.registrations.InternalRegistrationManagment;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
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
public class RegistrationReqAttributeFacility extends BaseFacility implements ConfirmationFacility
{
	private static final Logger log = Log.getLogger(Log.U_SERVER,
			RegistrationReqAttributeFacility.class);

	public static final String NAME = "registrationRequestVerificator";

	protected RegistrationRequestDB requestDB;
	private RegistrationFormDB formsDB;
	protected InternalRegistrationManagment internalRegistrationManagment;

	@Autowired
	public RegistrationReqAttributeFacility(DBSessionManager db,
			RegistrationRequestDB requestDB, RegistrationFormDB formsDB,
			InternalRegistrationManagment internalRegistrationManagment)
	{
		super(db);
		this.requestDB = requestDB;
		this.formsDB = formsDB;
		this.internalRegistrationManagment = internalRegistrationManagment;
	}

	@Override
	public String getName()
	{
		return RegistrationReqAttribiuteConfirmationState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms attributes from registration request with verifiable values";
	}

	private RegistrationReqAttribiuteConfirmationState getState(String state)
	{
		RegistrationReqAttribiuteConfirmationState attrState = new RegistrationReqAttribiuteConfirmationState();
		attrState.setSerializedConfiguration(state);
		return attrState;
	}

	protected ConfirmationStatus confirmElements(RegistrationRequest req, String state)
			throws EngineException
	{
		RegistrationReqAttribiuteConfirmationState attrState = getState(state);
		Collection<Attribute<?>> confirmedList = confirmAttributes(req.getAttributes(),
				attrState.getType(), attrState.getGroup(), attrState.getValue());
		boolean confirmed = (confirmedList.size() > 0);
		return new ConfirmationStatus(confirmed,
				confirmed ? "ConfirmationStatus.successAttribute"
						: "ConfirmationStatus.attributeChanged",
				attrState.getType());

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConfirmationStatus processConfirmation(String state) throws EngineException
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
		
		if (reqState.getStatus().equals(RegistrationRequestStatus.rejected))
			return new ConfirmationStatus(false, "ConfirmationStatus.requestRejected");
		
		ConfirmationStatus status;
		SqlSession sql = db.getSqlSession(true);
		try
		{
			RegistrationRequest req = reqState.getRequest();
			status = confirmElements(req, state);
			requestDB.update(requestId, reqState, sql);
			sql.commit();

			if (status.isSuccess()
					&& reqState.getStatus().equals(RegistrationRequestStatus.pending)
					&& internalRegistrationManagment
							.checkAutoAcceptCondition(req))

			{
				RegistrationForm form = formsDB.get(req.getFormId(), sql);
				AdminComment internalComment = new AdminComment(
						InternalRegistrationManagment.AUTO_ACCEPT_COMMENT,
						0, false);
				reqState.getAdminComments().add(internalComment);
				log.debug("Accept registration request " + baseState.getOwner()
						+ " after confirmation [" + baseState.getType()
						+ "]" + baseState.getValue() + " by "
						+ baseState.getFacilityId());
				internalRegistrationManagment.acceptRequest(form, reqState, null,
						internalComment, true, sql);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processAfterSendRequest(String state) throws EngineException
	{
		RegistrationReqAttribiuteConfirmationState attrState = getState(state);
		String requestId = attrState.getOwner();
		RegistrationRequestState reqState = internalRegistrationManagment
				.getRequest(requestId);
		for (Attribute<?> attr : reqState.getRequest().getAttributes())
		{
			if (attr.getAttributeSyntax().isVerifiable())
			{
				for (Object val : attr.getValues())
				{
					updateConfirmationInfo((VerifiableElement) val,
							attrState.getValue());
				}
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
