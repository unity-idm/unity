/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications.email;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.notifications.CommunicationTechnology;
import pl.edu.icm.unity.base.registration.UserRequestState;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.notifications.NotificationChannelInstance;
import pl.edu.icm.unity.engine.notifications.NotificationFacility;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.store.api.IdentityDAO;

/**
 * Email notification facility.
 * @author K. Benedyczak
 */
@Component
public class EmailFacility implements NotificationFacility
{
	static final Logger log = Log.getLogger(Log.U_SERVER_NOTIFY, EmailFacility.class);
	public static final String NAME = "EMAIL";
	
	static final String CFG_USER = "mailx.smtp.auth.username"; 
	static final String CFG_PASSWD = "mailx.smtp.auth.password";
	static final String CFG_TRUST_ALL = "mailx.smtp.trustAll";
	
	private ExecutorsService executorsService;
	private PKIManagement pkiManagement;
	private AttributesHelper attributesHelper;
	private IdentityDAO dbIdentities;
	private EntityResolver idResolver;
	private AttributeTypeHelper atHelper;
	
	
	@Autowired
	public EmailFacility(ExecutorsService executorsService, PKIManagement pkiManagement, 
			AttributesHelper attributeHelper, EntityResolver idResolver, IdentityDAO dbIdentities,
			AttributeTypeHelper atSyntaxRegistry)
	{
		this.executorsService = executorsService;
		this.pkiManagement = pkiManagement;
		this.attributesHelper = attributeHelper;
		this.idResolver = idResolver;
		this.dbIdentities = dbIdentities;
		this.atHelper = atSyntaxRegistry;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Sends notifications by e-mail";
	}

	@Override
	public CommunicationTechnology getTechnology()
	{
		return CommunicationTechnology.EMAIL;
	}

	@Override
	public void validateConfiguration(String configuration) throws WrongArgumentException
	{
		Properties props = new Properties();
		try
		{
			props.load(new StringReader(configuration));
		} catch (IOException e)
		{
			throw new WrongArgumentException("Email configuration is invalid: " +
					"not a valid properties syntax was used", e);
		}
	}

	@Override
	public NotificationChannelInstance getChannel(String configuration)
	{
		return new EmailChannel(configuration, executorsService, pkiManagement);
	}

	/**
	 * Address is established as follows (first found is returned):
	 * <ol> 
	 * <li> entity's identity of email type, confirmed
	 * <li> entity's attribute selected as contact email, confirmed
	 * <li> entity's identity of email type
	 * <li> entity's attribute selected as contact email
	 * </ol>
	 * In each case if there are more then one addresses the first in the list is returned.
	 */
	@Override
	public String getAddressForEntity(EntityParam recipient, String preferredAddress, boolean onlyConfirmed)
			throws EngineException
	{
		List<VerifiableEmail> emailIds = getEmailIdentities(recipient);
		AttributeExt emailAttr = attributesHelper.getAttributeByMetadata(recipient, "/", 
				ContactEmailMetadataProvider.NAME);		
		
		if (preferredAddress != null && isPresent(preferredAddress, emailIds, emailAttr, onlyConfirmed))
			return preferredAddress;
		
		String confirmedAddress= getAddressFrom(emailIds, emailAttr, true);
		if (confirmedAddress != null)
			return confirmedAddress;
		if (!onlyConfirmed)
		{
			String plain = getAddressFrom(emailIds, emailAttr, false);
			if (plain != null)
				return plain;
		}
		throw new IllegalIdentityValueException("The entity does not have the"
				+ (onlyConfirmed ? " confirmed" : "") + " email address specified");
	}
	
	private boolean isPresent(String address, List<VerifiableEmail> emailIds,
			AttributeExt emailAttr, boolean onlyConfirmed)
	{
		for (VerifiableEmail ve : emailIds)
		{
			if (ve.getValue().equals(address))
			{
				if (onlyConfirmed)
				{
					if (ve.isConfirmed())
						return true;
				} else
				{
					return true;
				}
			}

		}

		if (emailAttr != null)
		{
			for (String emailO : emailAttr.getValues())
			{
				if (emailO.equals(address))
				{

					if (onlyConfirmed)
					{
						AttributeValueSyntax<?> syntax = atHelper
								.getUnconfiguredSyntax(emailAttr
										.getValueSyntax());
						if (syntax.isEmailVerifiable())
						{
							VerifiableEmail email = (VerifiableEmail) syntax
									.convertFromString(emailO);
							if (email.isConfirmed())
								return true;
						}
					} else
					{
						return true;
					}

				}

			}
		}
		return false;
	}

	/**
	 * Address is established as in
	 * {@link #getAddressForEntity(EntityParamSession)} however only the
	 * input from the registration request is used and the cases with
	 * "confirmed" status are skipped.
	 */
	@Override
	public String getAddressForUserRequest(UserRequestState<?> currentRequest)
			throws EngineException
	{
		List<VerifiableEmail> emailIds = getEmailIdentities(currentRequest);
		Attribute emailAttr = getEmailAttributeFromRequest(currentRequest);

		return getAddressFrom(emailIds, emailAttr, false);
	}

	private String getAddressFrom(List<VerifiableEmail> emailIds, Attribute emailAttr,
			boolean useConfirmed)
	{
		for (VerifiableEmail id : emailIds)
			if (!useConfirmed || id.isConfirmed())
				return id.getValue();

		if (emailAttr != null)
		{
			AttributeValueSyntax<?> syntax = atHelper
					.getUnconfiguredSyntax(emailAttr.getValueSyntax());
			if (!useConfirmed || syntax.isEmailVerifiable())
			{
				for (String emailO : emailAttr.getValues())
				{
					if (syntax.isEmailVerifiable())
					{
						VerifiableEmail email = (VerifiableEmail) syntax
								.convertFromString(emailO);
						if (!useConfirmed || email.isConfirmed())
							return email.getValue();
					} else if (!useConfirmed)
					{
						return emailO.toString();
					}
				}
			}
		}
		return null;
	}

	private List<VerifiableEmail> getEmailIdentities(EntityParam recipient) throws EngineException
	{
		List<VerifiableEmail> emailIds = new ArrayList<>();
		long entityId = idResolver.getEntityId(recipient);
		List<Identity> identities = dbIdentities.getByEntity(entityId);
		for (Identity id: identities)
			if (id.getTypeId().equals(EmailIdentity.ID))
				emailIds.add(EmailIdentity.fromIdentityParam(id));
		return emailIds;
	}

	private List<VerifiableEmail> getEmailIdentities(UserRequestState<?> currentRequest) throws EngineException
	{
		List<VerifiableEmail> emailIds = new ArrayList<>();
		List<IdentityParam> identities = currentRequest.getRequest().getIdentities();
		if (identities == null)
			return emailIds;
		for (IdentityParam id: identities)
			if (id != null && id.getTypeId().equals(EmailIdentity.ID))
				emailIds.add(EmailIdentity.fromIdentityParam(id));
		return emailIds;
	}
	
	private Attribute getEmailAttributeFromRequest(UserRequestState<?> currentRequest)
			throws EngineException
	{
		List<Attribute> attrs = currentRequest.getRequest().getAttributes();
		if (attrs == null)
			return null;
		AttributeType at = attributesHelper.getAttributeTypeWithSingeltonMetadata(
				ContactEmailMetadataProvider.NAME);
		if (at == null)
			return null;
		for (Attribute ap : attrs)
		{
			if (ap == null)
				continue;
			if (ap.getName().equals(at.getName()) && ap.getGroupPath().equals("/"))
				return ap;
		}
		return null;
	}
}
