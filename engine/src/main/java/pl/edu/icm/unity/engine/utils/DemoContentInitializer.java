/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.utils;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.ServerInitializer;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.utils.InitializerCommon;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

/**
 * Populates DB with some demo contents.
 * @author K. Benedyczak
 */
@Component
public class DemoContentInitializer implements ServerInitializer
{
	private static Logger log = Log.getLogger(Log.U_SERVER, DemoContentInitializer.class);
	public static final String NAME = "demoInitializer";
	private GroupsManagement groupsMan;
	private EntityManagement idsMan;
	private AttributesManagement attrMan;
	private InitializerCommon commonInitializer;
	private UnityMessageSource msg;
	private EntityCredentialManagement eCredMan;
	private AttributeTypeManagement atMan;
	
	@Autowired
	public DemoContentInitializer(@Qualifier("insecure") GroupsManagement groupsMan, 
			@Qualifier("insecure") EntityManagement idsMan,
			@Qualifier("insecure") EntityCredentialManagement eCredMan,
			@Qualifier("insecure") AttributesManagement attrMan, 
			@Qualifier("insecure") AttributeTypeManagement atMan,
			InitializerCommon commonInitializer,
			UnityMessageSource msg)
	{
		this.eCredMan = eCredMan;
		this.atMan = atMan;
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.idsMan = idsMan;
		this.attrMan = attrMan;
		this.commonInitializer = commonInitializer;
	}
	
	@Override
	public void run()
	{
		try
		{
			commonInitializer.initializeCommonAttributeTypes();
			commonInitializer.assignCnToAdmin();
			
			GroupContents rootContents = groupsMan.getContents("/", GroupContents.GROUPS);
			if (rootContents.getSubGroups().contains("/A"))
			{
				log.error("Seems that demo contents is installed, skipping");
				return;
			}
			groupsMan.addGroup(new Group("/A"));
			groupsMan.addGroup(new Group("/A/B"));
			groupsMan.addGroup(new Group("/A/B/C"));
			groupsMan.addGroup(new Group("/D"));
			groupsMan.addGroup(new Group("/D/E"));
			groupsMan.addGroup(new Group("/D/G"));
			groupsMan.addGroup(new Group("/D/F"));
			
			AttributeType userPicture = new AttributeType("picture", JpegImageAttributeSyntax.ID, msg);
			JpegImageAttributeSyntax picSyntax = new JpegImageAttributeSyntax();
			picSyntax.setMaxSize(1400000);
			picSyntax.setMaxWidth(900);
			picSyntax.setMaxHeight(900);
			userPicture.setValueSyntaxConfiguration(picSyntax.getSerializedConfiguration());
			userPicture.setMaxElements(10);
			atMan.addAttributeType(userPicture);

			AttributeType name = new AttributeType("name", StringAttributeSyntax.ID, msg);
			name.setMinElements(1);
			StringAttributeSyntax namesyntax = new StringAttributeSyntax();
			namesyntax.setMaxLength(100);
			namesyntax.setMinLength(2);
			name.setValueSyntaxConfiguration(namesyntax.getSerializedConfiguration());
			atMan.addAttributeType(name);

			
			AttributeType postalcode = new AttributeType("postalcode", StringAttributeSyntax.ID, msg);
			postalcode.setMinElements(0);
			postalcode.setMaxElements(Integer.MAX_VALUE);
			StringAttributeSyntax pcsyntax = new StringAttributeSyntax();
			pcsyntax.setRegexp("[0-9][0-9]-[0-9][0-9][0-9]");
			pcsyntax.setMaxLength(6);
			postalcode.setValueSyntaxConfiguration(pcsyntax.getSerializedConfiguration());
			atMan.addAttributeType(postalcode);
			
			
			
			AttributeType height = new AttributeType("height", FloatingPointAttributeSyntax.ID, msg);
			height.setMinElements(1);
			atMan.addAttributeType(height);

			IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "demo-user");
			Identity base = idsMan.addEntity(toAdd, "Password requirement", EntityState.valid, false);

			IdentityParam toAddDn = new IdentityParam(X500Identity.ID, "CN=Demo user");
			idsMan.addIdentity(toAddDn, new EntityParam(base.getEntityId()), true);

			groupsMan.addMemberFromParent("/A", new EntityParam(base.getEntityId()));

			Attribute a = EnumAttribute.of("sys:AuthorizationRole", "/", "Regular User");
			attrMan.setAttribute(new EntityParam(base.getEntityId()), a, false);

			Attribute orgA = StringAttribute.of("o", "/", "Example organization", "org2", "org3");
			attrMan.setAttribute(new EntityParam(base.getEntityId()), orgA, false);

			VerifiableEmail emailVal = new VerifiableEmail("some@email.com", new ConfirmationInfo(true));
			emailVal.getConfirmationInfo().setConfirmationDate(System.currentTimeMillis());
			emailVal.getConfirmationInfo().setConfirmed(true);
			Attribute emailA = VerifiableEmailAttribute.of(
					InitializerCommon.EMAIL_ATTR, "/", emailVal);
			attrMan.setAttribute(new EntityParam(base.getEntityId()), emailA, false);

			Attribute cnA = StringAttribute.of("cn", "/", "Hiper user");
			attrMan.setAttribute(new EntityParam(base.getEntityId()), cnA, false);

			PasswordToken pToken = new PasswordToken("the!test1");
			eCredMan.setEntityCredential(new EntityParam(base.getEntityId()), "Password credential", 
					pToken.toJson());
		} catch (Exception e)
		{
			log.warn("Error loading demo contents. This can happen and by far is not critical. " +
					"It means that demonstration contents was not loaded to your database, " +
					"usaully due to conflict with its existing data", e);
		}
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
