module com.herb.rhythm.rhythemgamelevels {
    requires java.desktop;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;


    opens com.herb.rhythm.rhythemgamelevels to javafx.fxml;
    exports com.herb.rhythm.rhythemgamelevels;
}
