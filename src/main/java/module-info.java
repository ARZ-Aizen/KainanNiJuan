module com.kainanresto {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    // DB
    requires java.sql;
    requires org.mariadb.jdbc;
    requires com.zaxxer.hikari;


    // JavaFX FXML loader
    opens com.kainanresto to javafx.fxml;
    opens com.kainanresto.controllers to javafx.fxml;

    // Model
    opens com.kainanresto.model to javafx.base;

    // Exported packages for access across modules
    exports com.kainanresto;
    exports com.kainanresto.controllers;
    exports com.kainanresto.config;
    exports com.kainanresto.dao;
    exports com.kainanresto.model;
    exports com.kainanresto.util;


}