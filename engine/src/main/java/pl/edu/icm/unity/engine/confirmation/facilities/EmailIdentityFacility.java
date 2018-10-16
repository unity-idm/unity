/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmation.facilities;

import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.CONFIRMATION_DEFAULT_RETURN_URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder.ConfirmedElementType;
import pl.edu.icm.unity.engine.api.confirmation.states.EmailIdentityConfirmationState;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.identity.IdentityTypeHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TxManager;
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Identity confirmation facility.
 * 
 * @author P. Piernik
 */
@Component
public class EmailIdentityFacility extends UserEmailFacility<EmailIdentityConfirmationState>
{
	private IdentityTypeHelper identityTypeHelper;
	private IdentityDAO idDAO;
	private TxManager txMan;
	private boolean autoRedirect;

	@Autowired
	protected EmailIdentityFacility(EntityDAO dbIdentities, IdentityTypeHelper identityTypeHelper,
			IdentityDAO idDAO, TxManager tx, UnityMessageSource msg, UnityServerConfiguration serverConfig)
	{
		super(dbIdentities, msg, serverConfig.getValue(CONFIRMATION_DEFAULT_RETURN_URL));
		this.identityTypeHelper = identityTypeHelper;
		this.idDAO = idDAO;
		this.txMan = tx;
		this.autoRedirect = serverConfig.getBooleanValue(UnityServerConfiguration.CONFIRMATION_AUTO_REDIRECT);
	}

	@Override
	public String getName()
	{
		return EmailIdentityConfirmationState.FACILITY_ID;
	}

	@Override
	public String getDescription()
	{
		return "Confirms verifiable identity";
	}

	@Override
	protected WorkflowFinalizationConfiguration confirmElements(EmailIdentityConfirmationState idState) 
			throws EngineException
	{
		List<Identity> ids = idDAO.getByEntity(idState.getOwnerEntityId());

		ArrayList<Identity> idsA = new ArrayList<>();
		for (Identity id : ids)
		{
			if (identityTypeHelper.getTypeDefinition(id.getTypeId()).isEmailVerifiable())
				idsA.add(id);
		}

		Collection<Identity> confirmedList = confirmIdentity(idsA,
				idState.getType(), idState.getValue());
		for (Identity id : confirmedList)
		{
			idDAO.update(new StoredIdentity(id));
		}
		
		txMan.commit();
		
		boolean confirmed = (confirmedList.size() > 0);
		String title = msg.getMessage(confirmed ? 
				"ConfirmationStatus.successTitle" : "ConfirmationStatus.unsuccessful");
		String info = msg.getMessage(confirmed ? 
				"ConfirmationStatus.successDetail" : "ConfirmationStatus.emailChanged", idState.getValue());
		String redirectURL = confirmed ? getSuccessRedirect(idState) : getErrorRedirect(idState);
		return WorkflowFinalizationConfiguration.builder()
				.setAutoRedirect(autoRedirect)
				.setSuccess(confirmed)
				.setMainInformation(title)
				.setExtraInformation(info)
				.setRedirectURL(redirectURL)
				.build();
	}

	@Override
	@Transactional
	public void processAfterSendRequest(String state) throws EngineException
	{
		EmailIdentityConfirmationState idState = new EmailIdentityConfirmationState(state);
		List<Identity> ids = idDAO.getByEntity(idState.getOwnerEntityId());
		for (Identity id : ids)
		{
			if (id.getTypeId().equals(idState.getType()) && id.getValue().equals(idState.getValue()))
			{
				updateConfirmationInfo(id, idState.getValue());
				idDAO.update(new StoredIdentity(id));
			}
		}
	}

	@Override
	public EmailIdentityConfirmationState parseState(String state)
	{
		return new EmailIdentityConfirmationState(state);
	}

	@Override
	protected ConfirmedElementType getConfirmedElementType(EmailIdentityConfirmationState state)
	{
		return ConfirmedElementType.identity;
	}
}
