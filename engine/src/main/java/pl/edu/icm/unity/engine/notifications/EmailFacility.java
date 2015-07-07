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

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.engine.internal.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationStatus;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.stdext.utils.EmailUtils;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

import com.sun.mail.util.MailSSLSocketFactory;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.impl.SocketFactoryCreator;

/**
 * Email notification facility.
 * @author K. Benedyczak
 */
@Component
public class EmailFacility implements NotificationFacility
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, EmailFacility.class);
	public static final String NAME = "email";
	
	private static final String CFG_USER = "mailx.smtp.auth.username"; 
	private static final String CFG_PASSWD = "mailx.smtp.auth.password";
	private static final String CFG_TRUST_ALL = "mailx.smtp.trustAll";
	
	private ExecutorsService executorsService;
	private PKIManagement pkiManagement;
	private AttributesHelper attributesHelper;
	private DBIdentities dbIdentities;
	private IdentitiesResolver idResolver;
	
	@Autowired
	public EmailFacility(ExecutorsService executorsService, PKIManagement pkiManagement, 
			AttributesHelper attributeHelper, IdentitiesResolver idResolver, DBIdentities dbIdentities)
	{
		this.executorsService = executorsService;
		this.pkiManagement = pkiManagement;
		this.attributesHelper = attributeHelper;
		this.idResolver = idResolver;
		this.dbIdentities = dbIdentities;
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
		// TODO create properties helper and validate more throughly?
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
	 * <li> entity's identity of email type tagged with EmailUtils.TAG_MAIN, confirmed
	 * <li> entity's attribute selected as contact email tagged with EmailUtils.TAG_MAIN, confirmed
	 * <li> entity's identity of email type, confirmed
	 * <li> entity's attribute selected as contact email, confirmed
	 * <li> entity's identity of email type tagged with EmailUtils.TAG_MAIN
	 * <li> entity's attribute selected as contact email tagged with EmailUtils.TAG_MAIN
	 * <li> entity's identity of email type
	 * <li> entity's attribute selected as contact email
	 * </ol>
	 * In each case if there are more then one addresses the first in the list is returned.
	 */
	@Override
	public String getAddressForEntity(EntityParam recipient, SqlSession sql)
			throws EngineException
	{
		List<VerifiableEmail> emailIds = getEmailIdentities(recipient, sql);
		AttributeExt<?> emailAttr = attributesHelper.getAttributeByMetadata(recipient, "/", 
				ContactEmailMetadataProvider.NAME, sql);		
		
		String mainAndConfirmed = getAddressFrom(emailIds, emailAttr, true, true);
		if (mainAndConfirmed != null)
			return mainAndConfirmed;

		String confirmedOnly = getAddressFrom(emailIds, emailAttr, false, true);
		if (confirmedOnly != null)
			return confirmedOnly;
		
		String mainOnly = getAddressFrom(emailIds, emailAttr, true, false);
		if (mainOnly != null)
			return mainOnly;

		String plain = getAddressFrom(emailIds, emailAttr, false, false);
		if (plain != null)
			return plain;

		throw new IllegalIdentityValueException("The entity does not have the email address specified");
	}

	/**
	 * Address is established as in {@link #getAddressForEntity(EntityParam, SqlSession)} however only the input
	 * from the registration request is used and the cases with "confirmed" status are skipped.
	 */ 
	@Override
	public String getAddressForRegistrationRequest(RegistrationRequestState currentRequest,
			SqlSession sql) throws EngineException
	{
		List<VerifiableEmail> emailIds = getEmailIdentities(currentRequest);
		Attribute<?> emailAttr = getEmailAttributeFromRequest(currentRequest, sql); 

		String main = getAddressFrom(emailIds, emailAttr, true, false);
		if (main != null)
			return main;
		
		return getAddressFrom(emailIds, emailAttr, false, false);
	}
	
	
	private String getAddressFrom(List<VerifiableEmail> emailIds, Attribute<?> emailAttr, boolean useMain, 
			boolean useConfirmed)
	{
		for (VerifiableEmail id: emailIds)
			if ((!useConfirmed || id.isConfirmed()) && 
					(!useMain || id.getTags().contains(EmailUtils.TAG_MAIN)))
				return id.getValue();

		if (emailAttr != null && (!useConfirmed || emailAttr.getAttributeSyntax().isVerifiable()))
			for (Object emailO: emailAttr.getValues())
			{
				VerifiableEmail email = (VerifiableEmail) emailO;
				if ((!useConfirmed || email.isConfirmed()) && 
						(!useMain || email.getTags().contains(EmailUtils.TAG_MAIN)))
					return email.getValue();
			}
		return null;
	}
	
	private List<VerifiableEmail> getEmailIdentities(EntityParam recipient, SqlSession sql) throws EngineException
	{
		List<VerifiableEmail> emailIds = new ArrayList<>();
		long entityId = idResolver.getEntityId(recipient, sql);
		Identity[] identities = dbIdentities.getIdentitiesForEntityNoContext(entityId, sql);
		for (Identity id: identities)
			if (id.getTypeId().equals(EmailIdentity.ID))
				emailIds.add(EmailIdentity.fromIdentityParam(id));
		return emailIds;
	}

	private List<VerifiableEmail> getEmailIdentities(RegistrationRequestState currentRequest) throws EngineException
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
	
	private Attribute<?> getEmailAttributeFromRequest(RegistrationRequestState currentRequest, SqlSession sql)
			throws EngineException
	{
		List<Attribute<?>> attrs = currentRequest.getRequest().getAttributes();
		if (attrs == null)
			return null;
		AttributeType at = attributesHelper.getAttributeTypeWithSingeltonMetadata(
				ContactEmailMetadataProvider.NAME, sql);
		if (at == null)
			return null;
		for (Attribute<?> ap : attrs)
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
				final String msgSubject, final String message)
		{
			final NotificationStatus retStatus = new NotificationStatus();
			return executorsService.getService().submit(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						sendEmail(msgSubject, message, recipientAddress);
					} catch (Exception e)
					{
						log.error("E-mail notification failed", e);
						retStatus.setProblem(e);
					}
				}
			}, retStatus);
		}
		
		private void sendEmail(String subject, String body, String to) throws MessagingException
		{
			log.debug("Sending e-mail message to '" + to +"' with subject: " + subject);
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom();
			msg.setRecipients(Message.RecipientType.TO, to);
			msg.setSubject(subject);
			msg.setSentDate(new Date());
			msg.setText(body);
			Transport.send(msg);
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

		protected PasswordAuthentication getPasswordAuthentication() 
		{
			return new PasswordAuthentication(user, password);
		}
	}
}
