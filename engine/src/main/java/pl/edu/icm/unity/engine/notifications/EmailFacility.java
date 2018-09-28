/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications;

import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLSocketFactory;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.mail.util.MailSSLSocketFactory;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.impl.SocketFactoryCreator;
import pl.edu.icm.unity.base.notifications.FacilityName;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.notification.NotificationStatus;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.registration.UserRequestState;

/**
 * Email notification facility.
 * @author K. Benedyczak
 */
@Component
public class EmailFacility implements NotificationFacility
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, EmailFacility.class);
	public static final String NAME = FacilityName.EMAIL.toString();
	
	private static final String CFG_USER = "mailx.smtp.auth.username"; 
	private static final String CFG_PASSWD = "mailx.smtp.auth.password";
	private static final String CFG_TRUST_ALL = "mailx.smtp.trustAll";
	
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
		return new EmailChannel(configuration);
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
	
	private class EmailChannel implements NotificationChannelInstance
	{
		private Session session;
		
		public EmailChannel(String configuration)
		{
			Properties props = new Properties();
			try
			{
				props.load(new StringReader(configuration));
			} catch (IOException e)
			{
				//really shouldn't happen
				throw new IllegalStateException("Bug: can't load email properties " +
						"for the channel instance", e);
			}
			String smtpUser = props.getProperty(CFG_USER);
			String smtpPassword = props.getProperty(CFG_PASSWD);
			Authenticator smtpAuthn = (smtpUser != null && smtpPassword != null) ? 
					new SimpleAuthenticator(smtpUser, smtpPassword) : null;
			String trustAll = props.getProperty(CFG_TRUST_ALL);
			if (trustAll != null && "true".equalsIgnoreCase(trustAll))
			{
				MailSSLSocketFactory trustAllSF;
				try
				{
					trustAllSF = new MailSSLSocketFactory();
				} catch (GeneralSecurityException e)
				{
					//really shouldn't happen
					throw new IllegalStateException("Can't init trust-all SSL socket factory", e);
				}
				trustAllSF.setTrustAllHosts(true);
				props.put("mail.smtp.ssl.socketFactory", trustAllSF);
			} else
			{
				X509CertChainValidator validator = pkiManagement.getMainAuthnAndTrust().getValidator();
				SSLSocketFactory factory = SocketFactoryCreator.getSocketFactory(null, validator);
				props.put("mail.smtp.ssl.socketFactory", factory);
			}
			session = Session.getInstance(props, smtpAuthn);
		}
		
		@Override
		public Future<NotificationStatus> sendNotification(final String recipientAddress, 
				final MessageTemplate.Message message)
		{
			final NotificationStatus retStatus = new NotificationStatus();
			return executorsService.getService().submit(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						sendEmail(message, recipientAddress);
					} catch (Exception e)
					{
						log.error("E-mail notification failed", e);
						retStatus.setProblem(e);
					}
				}
			}, retStatus);
		}
		
		private void sendEmail(MessageTemplate.Message message, String to) throws MessagingException
		{
			log.debug("Sending e-mail message to '" + to +"' with subject: " + message.getSubject());
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom();
			msg.setRecipients(Message.RecipientType.TO, to);
			msg.setSubject(message.getSubject());
			msg.setSentDate(new Date());
			msg.setContent(message.getBody(), getContentType(message));
			Transport.send(msg);
		}

		private String getContentType(MessageTemplate.Message message)
		{
			switch (message.getType())
			{
			case HTML:
				return "text/html; charset=utf-8";
			case PLAIN:
				return "text/plain; charset=utf-8";
			}
			throw new IllegalStateException("BUG: missing conversion of type " 
					+ message.getType() + " to mail content type");
		}

		@Override
		public String getFacilityId()
		{
			return NAME;
		}
	}
	
	private static class SimpleAuthenticator extends Authenticator
	{
		private String user;
		private String password;
		
		public SimpleAuthenticator(String user, String password)
		{
			this.user = user;
			this.password = password;
		}

		@Override
		protected PasswordAuthentication getPasswordAuthentication() 
		{
			return new PasswordAuthentication(user, password);
		}
	}
}
