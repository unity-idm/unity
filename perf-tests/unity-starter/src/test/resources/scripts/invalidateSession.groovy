import pl.edu.icm.unity.engine.api.authn.LoginSession
import pl.edu.icm.unity.stdext.identity.UsernameIdentity
import pl.edu.icm.unity.types.basic.EntityParam
import pl.edu.icm.unity.types.basic.EntityState
import pl.edu.icm.unity.types.basic.IdentityParam

log.info("Invalidating session for {}", context);

IdentityParam identityParam = new IdentityParam(UsernameIdentity.ID, context);
LoginSession ownedSession = sessionManagement.getOwnedSession(new EntityParam(identityParam), "main");
sessionManagement.removeSession(ownedSession.getId(), true);
