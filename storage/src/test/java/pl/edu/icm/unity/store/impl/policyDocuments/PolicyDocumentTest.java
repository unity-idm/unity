/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.policyDocuments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;
import pl.edu.icm.unity.store.api.PolicyDocumentDAO;
import pl.edu.icm.unity.store.impl.AbstractBasicDAOTest;
import pl.edu.icm.unity.store.tx.TransactionTL;
import pl.edu.icm.unity.store.types.StoredPolicyDocument;

public class PolicyDocumentTest extends AbstractBasicDAOTest<StoredPolicyDocument>
{
	@Autowired
	private PolicyDocumentDAO dao;

	@Override
	protected PolicyDocumentDAO getDAO()
	{
		return dao;
	}

	@Override
	protected StoredPolicyDocument getObject(String name)
	{
		StoredPolicyDocument pd = new StoredPolicyDocument(1L, "name");
		pd.setDisplayedName(new I18nString("disp"));
		pd.setContent(new I18nString("content"));
		pd.setRevision(1);
		pd.setContentType(PolicyDocumentContentType.EMBEDDED);
		return pd;
	}

	@Override
	protected StoredPolicyDocument mutateObject(StoredPolicyDocument src)
	{
		src.setName("newName");
		src.setContent(new I18nString("content2"));
		src.setDisplayedName(new I18nString("disp2"));
		src.setRevision(1);
		return src;
	}
	
	@Test
	public void insertedWithIdIsReturned()
	{
		tx.runInTransaction(() -> {
			StoredPolicyDocument obj = getObject("name1");
			long key = obj.getId();
			dao.createWithId(key, obj);

			StoredPolicyDocument ret = dao.getByKey(key);

			assertThat(obj.getId()).isEqualTo(key);
			assertThat(ret).isNotNull();
			assertThat(ret).isEqualTo(obj);
		});
	}
	
	@Test
	public void uniqueIndexesAreAssigned()
	{
		tx.runInTransaction(() -> {
			long id1 = dao.create(new StoredPolicyDocument());
			long id2 = dao.create(new StoredPolicyDocument());

			assertThat(id1).isNotEqualTo(id2);
			
			TransactionTL.manualCommit();
			
			StoredPolicyDocument ret1 = dao.getByKey(id1);
			StoredPolicyDocument ret2 = dao.getByKey(id2);

			assertThat(ret1.getId()).isEqualTo(id1);
			assertThat(ret2.getId()).isEqualTo(id2);
		});
	}
	
	@Test
	public void regularInsertAfterInsertedWithIdSucceeds()
	{
		tx.runInTransaction(() -> {
			StoredPolicyDocument obj = getObject("name1");
			long key = obj.getId();
			dao.createWithId(key, obj);

			StoredPolicyDocument obj2 = getObject("");
			dao.create(obj2);
			
			assertThat(obj2.getId()).isNotEqualTo(key);
		});
	}
}
