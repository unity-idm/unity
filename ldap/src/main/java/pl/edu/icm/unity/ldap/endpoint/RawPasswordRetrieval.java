/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.apache.log4j.Logger;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.EntityWithCredential;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

/**
 * Retrieves raw passwords.
 * 
 * @author K. Benedyczak
 */
public class RawPasswordRetrieval extends AbstractCredentialRetrieval<PasswordExchange> implements LdapServerAuthentication
{
	private Logger log = Log.getLogger(Log.U_SERVER_LDAP, RawPasswordRetrieval.class);
	private UnityMessageSource msg;
	private CredentialEditorRegistry credEditorReg;
    private IdentityResolver identityResolver;

    static final String[] IDENTITY_TYPES = {UsernameIdentity.ID, EmailIdentity.ID};

	public RawPasswordRetrieval(UnityMessageSource msg, CredentialEditorRegistry credEditorReg, IdentityResolver identityResolver)
	{
		super(NAME);
		this.msg = msg;
		this.credEditorReg = credEditorReg;
        this.identityResolver = identityResolver;
	}

    @Override
	public String getSerializedConfiguration()
	{
		return null;
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		return;
	}

    public long verifyUser(String username) throws IllegalIdentityValueException {
        EntityWithCredential ewc = null;
        try
        {
            ewc = identityResolver.resolveIdentity(username, IDENTITY_TYPES, null);
            return ewc.getEntityId();
        } catch (Exception e){
            e.printStackTrace();
        }
        throw new IllegalIdentityValueException("No user");
    }

    public boolean checkPassword(String username, String password) {
        pl.edu.icm.unity.server.authn.AuthenticationResult ar = this.credentialExchange.checkPassword(
            username, password, null
        );
        return ar.getStatus().equals(AuthenticationResult.Status.success);
    }
}
