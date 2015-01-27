/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import java.util.Collection;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.states.RegistrationReqAttribiuteConfirmationState;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.engine.internal.InternalRegistrationManagment;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

/**
 * Attribute from registration request confirmation facility.
 * 
 * @author P. Piernik
 * 
 */
@Component
public class RegistrationReqAttributeFacility extends RegistrationFacility<RegistrationReqAttribiuteConfirmationState>
{
	public static final String NAME = "registrationRequestVerificator";

	@Autowired
	public RegistrationReqAttributeFacility(DBSessionManager db,
			RegistrationRequestDB requestDB, RegistrationFormDB formsDB,
			InternalRegistrationManagment internalRegistrationManagment)
	{
		super(db, requestDB, formsDB, internalRegistrationManagment);
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

	@Override
	protected ConfirmationStatus confirmElements(RegistrationRequest req, 
			RegistrationReqAttribiuteConfirmationState attrState) throws EngineException
	{
		Collection<Attribute<?>> confirmedList = confirmAttributes(req.getAttributes(),
				attrState.getType(), attrState.getGroup(), attrState.getValue());
		boolean confirmed = (confirmedList.size() > 0);
		return new ConfirmationStatus(confirmed, confirmed ? attrState.getSuccessUrl()
				: attrState.getErrorUrl(),
				confirmed ? "ConfirmationStatus.successAttribute"
						: "ConfirmationStatus.attributeChanged",
				attrState.getType());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processAfterSendRequest(String state) throws EngineException
	{
		RegistrationReqAttribiuteConfirmationState attrState = 
				new RegistrationReqAttribiuteConfirmationState(state);
		String requestId = attrState.getRequestId();
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

	@Override
	protected RegistrationReqAttribiuteConfirmationState parseState(String state)
	{
		return new RegistrationReqAttribiuteConfirmationState(state);
	}
}
