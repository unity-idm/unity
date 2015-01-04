package pl.edu.icm.unity.engine.confirmations;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.reg.RegistrationRequestDB;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.confirmations.ConfirmationFaciliity;
import pl.edu.icm.unity.server.api.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.server.api.confirmations.VerifiableElement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

public class RegistrationRequestFacility implements ConfirmationFaciliity
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			RegistrationRequestFacility.class);
	private final ObjectMapper mapper = Constants.MAPPER;
	public static final String NAME = "registrationRequestVerificator";

	private RegistrationRequestDB requestDB;
	private DBSessionManager db;
	
	@Autowired
	public RegistrationRequestFacility(DBSessionManager db, RegistrationRequestDB requestDB)
	{
		this.db = db;
		this.requestDB = requestDB;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Verify registration request attributes and indetities";
	}

	public String prepareState(String requestId, String attrType, String group)
			throws EngineException
	{

		ObjectNode state = mapper.createObjectNode();
		state.with("confirmationState");
		state.put("requestId", requestId);
		state.put("attrType", attrType);
		state.put("group", group);
		state.put("verificator", getName());
		try
		{
			return mapper.writeValueAsString(state);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}

	}

	@Override
	public ConfirmationStatus confirm(String state) throws EngineException
	{
		String requestId;
		String attrType;
		String group;
		try
		{
			ObjectNode main = mapper.readValue(state, ObjectNode.class);
			requestId = main.get("requestId").asText();
			attrType = main.get("attrType").asText();
			group = main.get("group").asText();

		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}

		SqlSession sql = db.getSqlSession(false);
		try
		{
			RegistrationRequestState reqState = requestDB.get(requestId, sql);
			RegistrationRequest req = reqState.getRequest();
			for (Attribute<?> attr : req.getAttributes())
			{
				if (attr.getName().equals(attrType)
						&& attr.getGroupPath().equals(group))
				{
					Attribute<VerifiableElement> vattr = (Attribute<VerifiableElement>) attr;
					vattr.getValues().get(0).setVerified(true);
				}
			}
			requestDB.update(requestId, reqState, sql);

		} finally
		{
			db.releaseSqlSession(sql);
		}

		return new ConfirmationStatus(true, "SUCCESSFULL CONFIRM ATTRIBUTE " + attrType);

	}
}
