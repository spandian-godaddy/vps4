package gdg.hfs.security;

import com.godaddy.vps4.security.Vps4User;

public interface Vps4UserService {

    Vps4User getUser(String shopperId);

    Vps4User getUser(long userId);

    Vps4User getOrCreateUserForShopper(String shopperId);

}