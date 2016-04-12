/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.Test;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.bulkops.action.RemoveEntityActionFactory;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.bulkops.ProcessingRuleParam;

/**
 * Invitations management test
 * @author Krzysztof Benedyczak
 */
public class TestBulkProcessing extends RESTAdminTestBase
{
	@Test
	public void submittedProcessingActionIsExecuted() throws Exception
	{
		IdentityParam identityParam = new IdentityParam(UsernameIdentity.ID, "user-to-remove");	
		idsMan.addEntity(identityParam, CRED_REQ_PASS, EntityState.valid, false);
		
		HttpPost post = new HttpPost("/restadm/v1/bulkProcessing/instant");
		ProcessingRuleParam param = new ProcessingRuleParam(
				"(idsByType contains 'userName') && (idsByType['userName'] contains 'user-to-remove')", 
				RemoveEntityActionFactory.NAME);
		String jsonform = m.writeValueAsString(param);
		System.out.println("Request to be sent:\n" + jsonform);
		post.setEntity(new StringEntity(jsonform, ContentType.APPLICATION_JSON));
		HttpResponse responsePost = client.execute(host, post, localcontext);

		assertEquals(Status.NO_CONTENT.getStatusCode(), responsePost.getStatusLine().getStatusCode());

		int wait = 0;
		do
		{
			Thread.sleep(500);
			catchException(idsMan).getEntity(new EntityParam(identityParam));
			
		} while (caughtException() == null && wait++ < 10);
		
		assertThat(caughtException(), isA(IllegalIdentityValueException.class));
	}
}
