/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.RegistrationConfirmationState;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.engine.internal.InternalRegistrationManagment;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.utils.JsonUtil;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Common code for processing verifiable elements for elements existing in registration (as opposed to 
 * elements bound to existing entitites).
 * 
 * @author K. Benedyczak
 */
public abstract class RegistrationFacility <T extends RegistrationConfirmationState> extends BaseFacility<T>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, RegistrationFacility.class);
	protected final ObjectMapper mapper = Constants.MAPPER;
	
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

	@Override
	public boolean isDuplicate(T base, String candidate)
	{
		ObjectNode main = JsonUtil.parse(candidate);
		if (!main.has("requestId"))
			return false;
		String requestId = main.get("requestId").asText();
		String value = main.get("value").asText();
		return base.getRequestId().equals(requestId) && base.getValue().equals(value);
	}
	
	protected abstract ConfirmationStatus confirmElements(RegistrationRequest req, T state) throws EngineException;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConfirmationStatus processConfirmation(String rawState, SqlSession sql) throws EngineException
	{
		T state = parseState(rawState);
		String requestId = state.getRequestId();

		RegistrationRequestState reqState = null;
		try
		{
			reqState = internalRegistrationManagment.getRequest(requestId, sql);

		} catch (EngineException e)
		{
			return new ConfirmationStatus(false, state.getErrorUrl() , "ConfirmationStatus.requestDeleted");
		}
		
		if (reqState.getStatus().equals(RegistrationRequestStatus.rejected))
			return new ConfirmationStatus(false, state.getErrorUrl(), "ConfirmationStatus.requestRejected");
		
		RegistrationRequest req = reqState.getRequest();
		ConfirmationStatus status = confirmElements(req, state);
		requestDB.update(requestId, reqState, sql);
		//make sure we update request, later on auto-acceptance may fail
		sql.commit();
		
		if (status.isSuccess()
				&& reqState.getStatus().equals(RegistrationRequestStatus.pending)
				&& internalRegistrationManagment.checkAutoAcceptCondition(req, sql))

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
			try
			{
				internalRegistrationManagment.acceptRequest(form, reqState, null,
					internalComment, true, sql);
			} catch (EngineException e)
			{
				sql.rollback();
				log.error("Automatic acceptance of the reqistration request "
						+ "(in effect of confirmation) failed", e);
			}
		}

		return status;
	}
	
}
