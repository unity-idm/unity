package pl.edu.icm.unity.store.migration.to4_1;

import static pl.edu.icm.unity.store.objstore.authn.AuthenticatorConfigurationHandler.AUTHENTICATOR_OBJECT_TYPE;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;
import pl.edu.icm.unity.store.objstore.authn.AuthenticatorConfigurationHandler;

import java.io.IOException;
import java.util.List;

@Component
public class InDBUpdateFromSchema20 implements InDBContentsUpdater
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema20.class);

	private final ObjectStoreDAO genericObjectsDAO;

	@Autowired
	public InDBUpdateFromSchema20(ObjectStoreDAO genericObjectsDAO)
	{
		this.genericObjectsDAO = genericObjectsDAO;
	}

	@Override
	public int getUpdatedVersion()
	{
		return 20;
	}

	@Override
	public void update() throws IOException
	{
		List<GenericObjectBean> authenticators = genericObjectsDAO.getObjectsOfType(AUTHENTICATOR_OBJECT_TYPE);

		for (GenericObjectBean authenticator : authenticators)
		{
			ObjectNode authenticatorNode = JsonUtil.parse(authenticator.getContents());
			UpdateHelperTo4_1.removeLocalOAuthCredential(authenticatorNode).ifPresent(updated ->
			{
				authenticator.setContents(JsonUtil.serialize2Bytes(updated));
				genericObjectsDAO.updateByKey(authenticator.getId(), authenticator);
				LOG.info("Removed legacy credential configuration from local OAuth RP authenticator {}", authenticator.getName());
			});
		}
	}
}
