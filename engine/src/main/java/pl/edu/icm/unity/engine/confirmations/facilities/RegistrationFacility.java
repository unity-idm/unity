/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.RegistrationConfirmationState;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.engine.registrations.InternalRegistrationManagment;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

/**
 * Common code for processing verifiable elements for elements existing in registration (as opposed to 
 * elements bound to existing entitites).
 * 
 * @author K. Benedyczak
 */
public abstract class RegistrationFacility <T extends RegistrationConfirmationState> extends BaseFacility
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, RegistrationFacility.class);
	
	protected RegistrationRequestDB requestDB;
	protected RegistrationFormDB formsDB;
	protected InternalRegistrationManagment internalRegistrationManagment;

	public RegistrationFacility(DBSessionManager db, RegistrationRequestDB requestDB,
			RegistrationFormDB formsDB,
			InternalRegistrationManagment internalRegistrationManagment)
	{
		super(db);
		this.requestDB = requestDB;
		this.formsDB = formsDB;
		this.internalRegistrationManagment = internalRegistrationManagment;
	}

	protected abstract T parseState(String state);
	protected abstract ConfirmationStatus confirmElements(RegistrationRequest req, T state) throws EngineException;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConfirmationStatus processConfirmation(String rawState) throws EngineException
	{
		T state = parseState(rawState);
		String requestId = state.getRequestId();

		RegistrationRequestState reqState = null;
		try
		{
			reqState = internalRegistrationManagment.getRequest(requestId);

		} catch (EngineException e)
		{
			return new ConfirmationStatus(false, state.getErrorUrl() , "ConfirmationStatus.requestDeleted");
		}
		
		if (reqState.getStatus().equals(RegistrationRequestStatus.rejected))
			return new ConfirmationStatus(false, state.getErrorUrl(), "ConfirmationStatus.requestRejected");
		
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
				log.debug("Accept registration request " + state.getRequestId()
						+ " after confirmation [" + state.getType()
						+ "]" + state.getValue() + " by "
						+ state.getFacilityId());
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
	
}
