/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations.facilities;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.confirmations.ConfirmationFacility;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.VerifiableElement;
import pl.edu.icm.unity.confirmations.states.AttribiuteFromRegState;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.reg.RegistrationFormDB;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.engine.registrations.InternalRegistrationManagment;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.registration.AdminComment;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

/**
 * Verifiable attribute from registration request facility.
 * 
 * @author P. Piernik
 * 
 */
public class AttributeFromRegistrationRequestFacility implements ConfirmationFacility
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			AttributeFromRegistrationRequestFacility.class);
	public static final String NAME = "registrationRequestVerificator";

	private RegistrationRequestDB requestDB;
	private DBSessionManager db;
	private RegistrationFormDB formsDB;
	private InternalRegistrationManagment internalRegistrationManagment;

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
	public String getId()
	{
		return AttribiuteFromRegState.FACILITY_ID;
	}

	@Override
	public ConfirmationStatus confirm(String state) throws EngineException
	{
		AttribiuteFromRegState attrState = new AttribiuteFromRegState();
		attrState.setSerializedConfiguration(state);

		SqlSession sql = db.getSqlSession(false);
		try
		{
			RegistrationRequestState reqState = requestDB
					.get(attrState.getOwner(), sql);
			RegistrationRequest req = reqState.getRequest();
			for (Attribute<?> attr : req.getAttributes())
			{
				if (attr.getName().equals(attrState.getType())
						&& attr.getGroupPath().equals(attrState.getGroup()))
				{
					Attribute<VerifiableElement> vattr = (Attribute<VerifiableElement>) attr;
					for (VerifiableElement el : vattr.getValues())
					{
						if (el.getValue().equals(attrState.getValue()))
						{
							el.setVerified(true);
						}
					}

				}
			}

			requestDB.update(attrState.getOwner(), reqState, sql);
			if (internalRegistrationManagment.checkAutoAcceptCondition(req))
			{
				//TODO
				RegistrationForm form = formsDB.get(req.getFormId(), sql);
				AdminComment internalComment = new AdminComment(
						"AUTO ACCEPT AFTER CONFIRM", 0, false);
				reqState.getAdminComments().add(internalComment);
				internalRegistrationManagment.acceptRequest(form, reqState, null,
						internalComment, sql);
			}

		} finally
		{
			db.releaseSqlSession(sql);
		}

		return new ConfirmationStatus(true, "SUCCESSFULL CONFIRM ATTRIBUTE "
				+ attrState.getType());
	}
}
