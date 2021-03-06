package org.openbase.bco.bcozy.controller;

import com.jfoenix.controls.JFXTreeTableView;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Accordion;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import org.openbase.bco.bcozy.BCozy;
import org.openbase.bco.bcozy.model.LanguageSelection;
import org.openbase.bco.bcozy.permissions.model.RecursiveUnitConfig;
import org.openbase.bco.bcozy.view.Constants;
import org.openbase.bco.bcozy.view.ForegroundPane;
import org.openbase.bco.bcozy.view.ObserverLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.util.Callback;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;

/**
 * @author vdasilva
 */
public class SettingsController {

    /**
     * Application Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsController.class);

    public Accordion adminAccordion;

    @FXML
    private TabPane tabPane;

    @FXML
    private Tab settingsTab;

    @FXML
    private Tab permissionTab;

    @FXML
    private JFXTreeTableView<RecursiveUnitConfig> unitsTable;

    @FXML
    private VBox permissionPaneParent;

    private UserSettingsController userSettingsController;

    /**
     * @deprecated Use {@link #SettingsController()} instead.
     */
    @Deprecated
    public SettingsController(ForegroundPane foregroudPane) {
    }

    public SettingsController() {
    }

    @FXML
    public void initialize() {
        settingsTab.setGraphic(new ObserverLabel("settings"));
        permissionTab.setGraphic(new ObserverLabel("permissions"));

        try {
            settingsTab.setContent(loadUserSettingsPane());
        } catch (CouldNotPerformException es) {
            ExceptionPrinter.printHistory(es, LOGGER);
        }

        try {
            permissionTab.setContent(loadPermissionPane());
        } catch (CouldNotPerformException es) {
            ExceptionPrinter.printHistory(es, LOGGER);
        }

        this.tabPane.widthProperty().addListener(this::onPaneWidthChange);
        onPaneWidthChange(null, null, null);
        this.tabPane.getStyleClass().addAll("detail-menu");

        try {
            TitledPane registrationPane = new TitledPane("userManagement", loadRegistrationPane());
            LanguageSelection.addObserverFor("userManagement", registrationPane::setText);
            this.adminAccordion.getPanes().add(registrationPane);
        } catch (CouldNotPerformException es) {
            ExceptionPrinter.printHistory(es, LOGGER);
        }

        try {
            TitledPane groupsPane = new TitledPane("usergroups", loadGroupsPane());
            LanguageSelection.addObserverFor("usergroups", groupsPane::setText);
            this.adminAccordion.getPanes().add(groupsPane);
        } catch (CouldNotPerformException es) {
            ExceptionPrinter.printHistory(es, LOGGER);
        }
    }

    public static <CONTROLLER> Pair<Pane, CONTROLLER> getFxmlPaneAndControllerPair(String filename, final Class clazz) throws CouldNotPerformException {
        return getFxmlPaneAndControllerPair(filename, clazz, null);
    }

    // TODO: move to jul
    public static <CONTROLLER> Pair<Pane, CONTROLLER> getFxmlPaneAndControllerPair(String filename, final Class clazz, final Callback<Class<?>, Object> controllerFactory) throws CouldNotPerformException {
        URL url;
        FXMLLoader loader;
        try {
            url = clazz.getResource(filename);
            if (url == null) {
                throw new NotAvailableException(filename);
            }
            loader = new FXMLLoader(url);
            if (controllerFactory != null) {
                loader.setControllerFactory(controllerFactory);
            }
            return new Pair<>(loader.load(), loader.getController());
        } catch (NullPointerException | IOException | CouldNotPerformException ex) {
            try {
                url = clazz.getClassLoader().getResource(filename);
                if (url == null) {
                    throw new NotAvailableException(filename);
                }
                loader = new FXMLLoader(url);
                if (controllerFactory != null) {
                    loader.setControllerFactory(controllerFactory);
                }
                return new Pair<>(loader.load(), loader.getController());
            } catch (NullPointerException | IOException | CouldNotPerformException exx) {
                MultiException.ExceptionStack exceptionStack = new MultiException.ExceptionStack();
                exceptionStack = MultiException.push(clazz, ex, exceptionStack);
                exceptionStack = MultiException.push(clazz, exx, exceptionStack);
                throw new MultiException("Could not load FXML[" + filename + "]", exceptionStack);
            }
        }
    }

    private <CONTROLLER> Pane loadUserSettingsPane() throws CouldNotPerformException {
        try {
            final Pair<Pane, UserSettingsController> paneAndControllerPair = getFxmlPaneAndControllerPair("UserSettingsPane.fxml", getClass());

            this.userSettingsController = paneAndControllerPair.getValue();

            this.userSettingsController.getThemeChoice().setOnAction(event -> chooseTheme());

            //Necessary to ensure that the first change is not missed by the ChangeListener
            this.userSettingsController.getThemeChoice().getSelectionModel().select(0);

            return paneAndControllerPair.getKey();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Content could not be loaded", ex);
        }
    }

    private <T> void onPaneWidthChange(ObservableValue<? extends T> observable, T oldValue, T newValue) {
        double width = this.tabPane.getWidth();
        double childrenCount = settingsTab.getTabPane().getTabs().size();

        settingsTab.getTabPane().setTabMinWidth(width / (childrenCount + 1));
        //+1 cause otherwise tabs would overlap with Floating Button 
    }

    public UserSettingsController getUserSettingsController() {
        return userSettingsController;
    }

    private void chooseTheme() {
        final ResourceBundle languageBundle = ResourceBundle.getBundle(Constants.LANGUAGE_RESOURCE_BUNDLE, Locale.getDefault());

        userSettingsController.getThemeChoice().getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(final ObservableValue<? extends Number> observableValue, final Number number, final Number number2) {
                if (userSettingsController.getAvailableThemes().get(number2.intValue()).equals(languageBundle.getString(Constants.LIGHT_THEME_CSS_NAME))) {
                    BCozy.changeTheme(Constants.LIGHT_THEME_CSS);
                } else if (userSettingsController.getAvailableThemes().get(number2.intValue()).equals(languageBundle.getString(Constants.DARK_THEME_CSS_NAME))) {
                    BCozy.changeTheme(Constants.DARK_THEME_CSS);
                }
            }
        });
    }

    private Pane loadPermissionPane() throws CouldNotPerformException {
        try {
            return getFxmlPaneAndControllerPair("view/permissions/PermissionsPane.fxml", getClass()).getKey();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not load permission pane", ex);
        }
    }

    private Pane loadGroupsPane() throws CouldNotPerformException {
        try {
            return getFxmlPaneAndControllerPair("GroupsPane.fxml", getClass()).getKey();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not load group pane", ex);
        }
    }

    private Pane loadRegistrationPane() throws CouldNotPerformException {
        try {
            return getFxmlPaneAndControllerPair("Registration.fxml", getClass(), clazz -> new UserManagementController()).getKey();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not load registration pane", ex);
        }
    }
}
