/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmation.facilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.confirmation.ConfirmationRedirectURLBuilder.ConfirmedElementType;
import pl.edu.icm.unity.engine.api.confirmation.ConfirmationStatus;
import pl.edu.icm.unity.engine.api.confirmation.states.IdentityConfirmationState;
import pl.edu.icm.unity.engine.identity.IdentityTypeHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TxManager;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Identity confirmation facility.
 * 
 * @author P. Piernik
 */
@Component
public class IdentityFacility extends UserFacility<IdentityConfirmationState>
{
	private IdentityTypeHelper identityTypeHelper;
	private IdentityDAO idDAO;
	private TxManager txMan;
	

	@Autowired
	protected IdentityFacility(EntityDAO dbIdentities, IdentityTypeHelper identityTypeHelper,
			IdentityDAO idDAO, TxManager tx)
	{
		super(dbIdentities);
		this.identityTypeHelper = identityTypeHelper;
		this.idDAO = idDAO;
		this.txMan = tx;
	}

	@Override
	public String getName()
	{
		return IdentityConfirmationState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms verifiable identity";
	}

	@Override
	protected ConfirmationStatus confirmElements(IdentityConfirmationState idState) 
			throws EngineException
	{
		List<Identity> ids = idDAO.getByEntity(idState.getOwnerEntityId());

		ArrayList<Identity> idsA = new ArrayList<>();
		for (Identity id : ids)
		{
			if (identityTypeHelper.getTypeDefinition(id.getTypeId()).isVerifiable())
				idsA.add(id);
		}

		Collection<Identity> confirmedList = confirmIdentity(idsA,
				idState.getType(), idState.getValue());
		for (Identity id : confirmedList)
		{
			idDAO.update(id);
		}
		
		txMan.commit();
		
		boolean confirmed = (confirmedList.size() > 0);
		ConfirmationStatus status = new ConfirmationStatus(confirmed, 
				confirmed ? getSuccessRedirect(idState) : getErrorRedirect(idState),
				confirmed ? "ConfirmationStatus.successIdentity"
						: "ConfirmationStatus.identityChanged",
						idState.getType());
		return status;
	}

	@Override
	@Transactional
	public void processAfterSendRequest(String state) throws EngineException
	{
		IdentityConfirmationState idState = new IdentityConfirmationState(state);
		List<Identity> ids = idDAO.getByEntity(idState.getOwnerEntityId());
		for (Identity id : ids)
		{
			if (id.getTypeId().equals(idState.getType()) && id.getValue().equals(idState.getValue()))
			{
				updateConfirmationInfo(id, idState.getValue());
				idDAO.update(id);
			}
		}
	}

	@Override
	public IdentityConfirmationState parseState(String state) throws WrongArgumentException
	{
		return new IdentityConfirmationState(state);
	}

	@Override
	protected ConfirmedElementType getConfirmedElementType(IdentityConfirmationState state)
	{
		return ConfirmedElementType.identity;
	}
}
