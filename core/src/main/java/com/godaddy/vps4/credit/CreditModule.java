package com.godaddy.vps4.credit;

import com.godaddy.vps4.credit.jdbc.JdbcVps4LocalCreditService;
import com.godaddy.vps4.prodMeta.ProdMetaService;
import com.godaddy.vps4.prodMeta.jdbc.JdbcProdMetaService;
import com.google.inject.AbstractModule;

public class CreditModule extends AbstractModule{

    @Override
    public void configure() {
        bind(CreditService.class).to(ECommCreditService.class);
        bind(ProdMetaService.class).to(JdbcProdMetaService.class);
        bind(Vps4LocalCreditService.class).to(JdbcVps4LocalCreditService.class);
    }
}
