/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.policyDocument;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentUpdateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;

public class TestPolicyDocument extends DBIntegrationTestBase
{
	@Autowired
	private PolicyDocumentManagement policyDocMan;

	private PolicyDocumentUpdateRequest generateUpdate(long id, String name)
	{
		return new PolicyDocumentUpdateRequest(id, name, new I18nString("dispu"), false,
				PolicyDocumentContentType.LINK, new I18nString("contu"));
	}

	private PolicyDocumentCreateRequest generateCreateReq(String name)
	{
		return new PolicyDocumentCreateRequest(name, new I18nString("disp"), true,
				PolicyDocumentContentType.EMBEDDED, new I18nString("cont"));
	}

	@Test
	public void shouldReturnCreated() throws EngineException
	{
		PolicyDocumentCreateRequest doc = generateCreateReq("test");
		long id = policyDocMan.addPolicyDocument(doc);
		PolicyDocumentWithRevision docRet = policyDocMan.getPolicyDocument(id);
		assertEqualDoc(doc, docRet);
	}

	@Test
	public void shouldReturnUpdatedWithRevsion() throws EngineException
	{
		PolicyDocumentCreateRequest doc = generateCreateReq("test");
		long id = policyDocMan.addPolicyDocument(doc);
		policyDocMan.updatePolicyDocumentWithRevision(generateUpdate(id, "test2"));
		PolicyDocumentWithRevision docRet = policyDocMan.getPolicyDocument(id);
		assertThat(docRet.name, is("test2"));
		assertThat(docRet.displayedName, is(new I18nString("dispu")));
		assertThat(docRet.content, is(new I18nString("contu")));
		assertThat(docRet.contentType, is(PolicyDocumentContentType.LINK));
		assertThat(docRet.mandatory, is(false));
		assertThat(docRet.revision, is(2));
	}

	@Test
	public void shouldReturnUpdatedWithoutRevsion() throws EngineException
	{
		PolicyDocumentCreateRequest doc = generateCreateReq("test");
		long id = policyDocMan.addPolicyDocument(doc);
		policyDocMan.updatePolicyDocument(generateUpdate(id, "test2"));
		PolicyDocumentWithRevision docRet = policyDocMan.getPolicyDocument(id);
		assertThat(docRet.name, is("test2"));
		assertThat(docRet.displayedName, is(new I18nString("dispu")));
		assertThat(docRet.content, is(new I18nString("contu")));
		assertThat(docRet.contentType, is(PolicyDocumentContentType.LINK));
		assertThat(docRet.mandatory, is(false));
		assertThat(docRet.revision, is(1));
	}

	@Test
	public void shouldReturnAll() throws EngineException
	{
		PolicyDocumentCreateRequest doc1 = generateCreateReq("test1");
		long id1 = policyDocMan.addPolicyDocument(doc1);

		PolicyDocumentCreateRequest doc2 = generateCreateReq("test2");
		long id2 = policyDocMan.addPolicyDocument(doc2);

		PolicyDocumentCreateRequest doc3 = generateCreateReq("test3");
		long id3 = policyDocMan.addPolicyDocument(doc3);

		Collection<PolicyDocumentWithRevision> policyDocuments = policyDocMan.getPolicyDocuments();

		assertThat(policyDocuments.size(), is(3));
		assertEqualDoc(doc1, policyDocuments.stream().filter(d -> d.id == id1).findFirst().get());
		assertEqualDoc(doc2, policyDocuments.stream().filter(d -> d.id == id2).findFirst().get());
		assertEqualDoc(doc3, policyDocuments.stream().filter(d -> d.id == id3).findFirst().get());

	}

	@Test
	public void shouldNotReturnRemoved() throws EngineException
	{
		PolicyDocumentCreateRequest doc1 = generateCreateReq("test1");
		long id1 = policyDocMan.addPolicyDocument(doc1);

		PolicyDocumentCreateRequest doc2 = generateCreateReq("test2");
		long id2 = policyDocMan.addPolicyDocument(doc2);

		policyDocMan.removePolicyDocument(id1);
		Collection<PolicyDocumentWithRevision> policyDocuments = policyDocMan.getPolicyDocuments();

		assertThat(policyDocuments.size(), is(1));
		assertEqualDoc(doc2, policyDocuments.stream().filter(d -> d.id == id2).findFirst().get());
	}

	private void assertEqualDoc(PolicyDocumentCreateRequest doc1, PolicyDocumentWithRevision doc2)
	{
		assertThat(doc1.name, is(doc2.name));
		assertThat(doc1.displayedName, is(doc2.displayedName));
		assertThat(doc1.content, is(doc2.content));
		assertThat(doc1.contentType, is(doc2.contentType));
		assertThat(doc1.mandatory, is(doc2.mandatory));
	}
}
