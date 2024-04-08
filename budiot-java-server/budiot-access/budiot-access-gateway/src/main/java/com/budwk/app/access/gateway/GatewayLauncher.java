package com.budwk.app.access.gateway;

import org.nutz.boot.NbApp;
import org.nutz.ioc.loader.annotation.IocBean;

@IocBean
public class GatewayLauncher {
    public static void main(String[] args) {

        NbApp nb = new NbApp().setArgs(args).setPrintProcDoc(true);
        nb.getAppContext().setMainPackage("com.budwk");
        nb.run();
    }

}
