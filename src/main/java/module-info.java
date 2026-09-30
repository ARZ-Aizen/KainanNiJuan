module com.kainanresto {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    //DB
    requires java.sql; //JAVA PANG CONNECT SA SQL TO
    requires org.mariadb.jdbc; //SI MARIADB/MYSQL
    requires com.zaxxer.hikari; //PARA SA PAG PROCESS TO PAG NAKA NETWORK
    requires jbcrypt; //ENCRYPTOR TO
    requires java.prefs; //PARA SA REMEMBER ME
    requires javafx.base;
    requires javafx.graphics;


    //FXML LOADER TO
    opens com.kainanresto to javafx.fxml;

    //MGA EXPORTED PACKAGES
    exports com.kainanresto;
    exports com.kainanresto.config;
    exports com.kainanresto.dao;
    exports com.kainanresto.util;
    exports com.kainanresto.controllers.id;
    opens com.kainanresto.controllers.id to javafx.fxml;
    exports com.kainanresto.controllers.main;
    opens com.kainanresto.controllers.main to javafx.fxml;
    exports com.kainanresto.controllers.util;
    opens com.kainanresto.controllers.util to javafx.fxml;
    exports com.kainanresto.model.account;
    opens com.kainanresto.model.account to javafx.base;
    exports com.kainanresto.model.dish;
    opens com.kainanresto.model.dish to javafx.base;
    exports com.kainanresto.model.order;
    opens com.kainanresto.model.order to javafx.base;
    exports com.kainanresto.model.util;
    opens com.kainanresto.model.util to javafx.base;
    exports com.kainanresto.model.transac;
    opens com.kainanresto.model.transac to javafx.base;


}