package com.godaddy.vps4.plesk;

import java.util.List;

import org.json.simple.parser.ParseException;

import com.godaddy.vps4.util.PollerTimedOutException;

public interface Vps4PleskService {

    List<PleskSubscription> listPleskAccounts(long hfsVmId)
            throws PleskUrlUnavailableException, ParseException, PollerTimedOutException, Exception;

    PleskSession getPleskSsoUrl(long hfsVmId, String fromIpAddress)
            throws PleskUrlUnavailableException, ParseException, PollerTimedOutException, Exception;


}
