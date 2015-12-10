/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.utils;

import java.util.Date;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.ServerInitializer;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.utils.InitializerCommon;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

/**
 * Populates DB with users contents.
 * @author Roman Krysinski
 */
@Component
public class MassUsersInitializer implements ServerInitializer
{
    private static Logger log = Log.getLogger(Log.U_SERVER, MassUsersInitializer.class);
    public static final String NAME = "massUsersInitializer";
    public static final int AMOUNT = 2000;
    private GroupsManagement groupsMan;
    private IdentitiesManagement idsMan;
    private AttributesManagement attrMan;
    private InitializerCommon commonInitializer;

    @Autowired
    public MassUsersInitializer(@Qualifier("insecure") GroupsManagement groupsMan,
            @Qualifier("insecure") IdentitiesManagement idsMan,
            @Qualifier("insecure") AttributesManagement attrMan, InitializerCommon commonInitializer)
    {
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
            if (rootContents.getSubGroups().contains("/R"))
            {
                log.error("Seems that demo contents is installed, skipping");
                return;
            }
            groupsMan.addGroup(new Group("/R"));

            for (int i = 0; i < AMOUNT; ++i)
            {
                IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "demo-user" + i);
                Identity base = idsMan.addEntity(toAdd, "Password requirement", EntityState.valid, false);

                IdentityParam toAddDn = new IdentityParam(X500Identity.ID, "CN=Demo user" + i);
                idsMan.addIdentity(toAddDn, new EntityParam(base.getEntityId()), true);

                groupsMan.addMemberFromParent("/R", new EntityParam(base.getEntityId()));

                EnumAttribute a = new EnumAttribute("sys:AuthorizationRole", "/", AttributeVisibility.local, "Regular User");
                attrMan.setAttribute(new EntityParam(base.getEntityId()), a, false);

                StringAttribute orgA = new StringAttribute("o", "/", AttributeVisibility.full,
                        "Example organization", "org" + i, "org3");
                attrMan.setAttribute(new EntityParam(base.getEntityId()), orgA, false);

                VerifiableEmailAttribute emailA = new VerifiableEmailAttribute(
                        InitializerCommon.EMAIL_ATTR, "/", AttributeVisibility.full,
                        new VerifiableEmail(i + "some@email.com", new ConfirmationInfo(true)));
                Date d = new Date();
                emailA.getValues().get(0).getConfirmationInfo().setConfirmationDate(d.getTime());
                emailA.getValues().get(0).getConfirmationInfo().setConfirmed(true);
                attrMan.setAttribute(new EntityParam(base.getEntityId()), emailA, false);

                StringAttribute cnA = new StringAttribute("cn", "/", AttributeVisibility.full, "Hiper user" + i);
                attrMan.setAttribute(new EntityParam(base.getEntityId()), cnA, false);

                PasswordToken pToken = new PasswordToken("the!test1");
                idsMan.setEntityCredential(new EntityParam(base.getEntityId()), "Password credential",
                        pToken.toJson());
            }
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