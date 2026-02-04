module SmartBloggingPlatform {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires java.naming;
    requires flyway.core;
    requires flyway.mysql;
    requires jbcrypt;
    requires com.github.benmanes.caffeine;
    requires com.zaxxer.hikari;
    requires org.slf4j;

    // MongoDB driver (automatic module names)
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.driver.core;
    requires org.mongodb.bson;

    opens com.smartblog.core.demo to javafx.fxml;

    // OPEN UI packages to FXML
    opens com.smartblog.ui.view.login to javafx.fxml;
    opens com.smartblog.ui.view.main to javafx.fxml;
    opens com.smartblog.ui.view.posts.dialog to javafx.fxml;
    opens com.smartblog.ui.view.posts.editor to javafx.fxml;
    opens com.smartblog.ui.view.posts to javafx.fxml;
    opens com.smartblog.ui.view.comments to javafx.fxml;
    opens com.smartblog.ui.view.tags to javafx.fxml;
    opens com.smartblog.ui.view.users to javafx.fxml;
    opens com.smartblog.ui.view.analytics to javafx.fxml;
    opens com.smartblog.ui.view.admin to javafx.fxml;
    opens com.smartblog.ui.view.performance to javafx.fxml;
    opens com.smartblog.ui.view.authors to javafx.fxml;

    exports com.smartblog;
    exports com.smartblog.bootstrap;
    exports com.smartblog.core.model;
}