/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.from2_4;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.identities.IdentitiesMapper;
import pl.edu.icm.unity.store.impl.identities.IdentityBean;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

/**
 * Update db from to 2.5.0 release version (DB schema version 2.3) from previous versions (schema 2.2)
 * @author P.Piernik
 */
@Component
public class InDBUpdateFromSchema2_2
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema2_2.class);

	@Autowired
	private ObjectStoreDAO genericObjectsDAO;
	
	public void update() throws IOException
	{
		updateEmailIdentitiesCmpValueToLowercase();
		dropChannelFromGenericForm("registrationForm");
		dropChannelFromGenericForm("enquiryForm");
		
		//FIXME add rest
		throw new IllegalStateException("Not implemented!");
	}

	
	
	private void dropChannelFromGenericForm(String objType)
	{
		List<GenericObjectBean> forms = genericObjectsDAO.getObjectsOfType(objType);
		for (GenericObjectBean form: forms)
		{
			ObjectNode objContent = JsonUtil.parse(form.getContents());
			
			//FIXME - should be shared code - returning Optional<ObjectNode> with updated obj. DB Update if present 
				ObjectNode notCfg = (ObjectNode) objContent
					.get("NotificationsConfiguration");
				if (notCfg.has("channel"))
				{
					notCfg.remove("channel");
				}
			
			//FIXME add logging of the change and condition as described above
			form.setContents(JsonUtil.serialize2Bytes(objContent));
			genericObjectsDAO.updateByKey(form.getId(), form);
		}
	}



	private void updateEmailIdentitiesCmpValueToLowercase()
	{
		IdentitiesMapper identityMapper = SQLTransactionTL.getSql().getMapper(IdentitiesMapper.class);
		List<IdentityBean> allIdentities = identityMapper.getAll();
		for (IdentityBean id: allIdentities)
		{
			if (id.getTypeName().equals("email"))
			{
				String value = new String(id.getContents(), StandardCharsets.UTF_8);
				String updated = new VerifiableEmail(value).getComparableValue();
				String inDbUpdated = StoredIdentity.toInDBIdentityValue("email", updated);
				if (!inDbUpdated.equals(id.getName()))
				{
					log.info("Updating email identity cmp value to lowercase {} -> {}",
							id.getName(), inDbUpdated);
					id.setName(inDbUpdated);
					identityMapper.updateByKey(id);
				}
			}
		}
	}
}
