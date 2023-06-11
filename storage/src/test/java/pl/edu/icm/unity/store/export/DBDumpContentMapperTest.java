/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.export;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import pl.edu.icm.unity.base.db.DBDumpContentElements;
import pl.edu.icm.unity.store.impl.identities.IdentityIE;
import pl.edu.icm.unity.store.objstore.cred.CredentialHandler;
import pl.edu.icm.unity.store.objstore.credreq.CredentialRequirementHandler;
import pl.edu.icm.unity.store.objstore.reg.invite.InvitationHandler;

/**
 * 
 * @author P.Piernik
 *
 */
public class DBDumpContentMapperTest
{
	@Test
	public void shouldGetAlsoDirectorySchemaWhenSignupRequests()
	{
		DBDumpContentElements ct = new DBDumpContentElements(false, false, false, false, true, false);

		List<String> ret = DBDumpContentTypeMapper.getDBElements(ct);
		// Dir schema
		assertThat(ret, hasItem(CredentialRequirementHandler.CREDENTIAL_REQ_OBJECT_TYPE));
		// SignupReq
		assertThat(ret, hasItem(InvitationHandler.INVITATION_OBJECT_TYPE));
		// System
		assertThat(ret, not(hasItem(CredentialHandler.CREDENTIAL_OBJECT_TYPE)));
		// Users
		assertThat(ret, not(hasItem(IdentityIE.IDENTITIES_OBJECT_TYPE)));
	}

	@Test
	public void shouldGetAlsoDirectorySchemaWhenUsers()
	{
		DBDumpContentElements ct = new DBDumpContentElements(false, false, true, false, false, false);

		List<String> ret = DBDumpContentTypeMapper.getDBElements(ct);
		// Dir schema
		assertThat(ret, hasItem(CredentialRequirementHandler.CREDENTIAL_REQ_OBJECT_TYPE));
		// Users
		assertThat(ret, hasItem(IdentityIE.IDENTITIES_OBJECT_TYPE));
		// SignupReq
		assertThat(ret, not(hasItem(InvitationHandler.INVITATION_OBJECT_TYPE)));
		// System
		assertThat(ret, not(hasItem(CredentialHandler.CREDENTIAL_OBJECT_TYPE)));
	}

	@Test
	public void shouldGetAlsoDirectorySchemaWhenClearUsers()
	{
		DBDumpContentElements ct = new DBDumpContentElements(false, false, true, false, false, false);

		List<String> ret = DBDumpContentTypeMapper.getElementsForClearDB(ct);
		// Dir schema
		assertThat(ret, hasItem(CredentialRequirementHandler.CREDENTIAL_REQ_OBJECT_TYPE));
		// Users
		assertThat(ret, hasItem(IdentityIE.IDENTITIES_OBJECT_TYPE));

	}

	@Test
	public void shouldGetAlsoDirectorySchemaWhenClearSignupRequests()
	{
		DBDumpContentElements ct = new DBDumpContentElements(false, false, false, false, true, false);

		List<String> ret = DBDumpContentTypeMapper.getElementsForClearDB(ct);
		// Dir schema
		assertThat(ret, hasItem(CredentialRequirementHandler.CREDENTIAL_REQ_OBJECT_TYPE));
		// SignupReq
		assertThat(ret, hasItem(InvitationHandler.INVITATION_OBJECT_TYPE));
	}
}
