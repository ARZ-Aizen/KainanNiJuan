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
    opens com.kainanresto.controllers to javafx.fxml;

    //SA DB MODEL TO
    opens com.kainanresto.model to javafx.base;

    //MGA EXPORTED PACKAGES
    exports com.kainanresto;
    exports com.kainanresto.controllers;
    exports com.kainanresto.config;
    exports com.kainanresto.dao;
    exports com.kainanresto.model;
    exports com.kainanresto.util;
    exports com.kainanresto.controllers.id;
    opens com.kainanresto.controllers.id to javafx.fxml;
    exports com.kainanresto.controllers.main;
    opens com.kainanresto.controllers.main to javafx.fxml;
    exports com.kainanresto.controllers.util;
    opens com.kainanresto.controllers.util to javafx.fxml;


}