/**
 * ==================================================================
 *
 * This file is part of org.openbase.bco.bcozy.
 *
 * org.openbase.bco.bcozy is free software: you can redistribute it and modify
 * it under the terms of the GNU General Public License (Version 3)
 * as published by the Free Software Foundation.
 *
 * org.openbase.bco.bcozy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with org.openbase.bco.bcozy. If not, see
 * <http://www.gnu.org/licenses/>.
 * ==================================================================
 */
package org.openbase.bco.bcozy.view.location;

import java.util.ArrayList;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import org.openbase.bco.bcozy.view.Constants;
import org.openbase.bco.bcozy.view.ForegroundPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.effect.Lighting;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.openbase.bco.bcozy.view.InfoPane;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.EnumNotSupportedException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author julian
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public final class LocationPane extends Pane {

    /**
     *
     * Singleton instance.
     */
    private static LocationPane instance;
    private static boolean initialized;

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationPane.class);

    private LocationPolygon selectedLocation;
    private ZonePolygon rootLocation;

    //private final StackPane backgroundPane;
    private final ForegroundPane foregroundPane;
    private final Map<String, TilePolygon> tileMap;
    private final Map<String, RegionPolygon> regionMap;
    private final Map<String, ZonePolygon> zoneMap;
    private final Map<String, ConnectionPolygon> connectionMap;
    private final List<Node> debugNodes;

    public final SimpleStringProperty selectedLocationId;

    private LocationPolygon lastFirstClickTarget;
    private LocationPolygon lastSelectedTile;
    private final EventHandler<MouseEvent> onEmptyAreaClickHandler;

    /**
     * Private constructor to deny manual instantiation.
     *
     * @param foregroundPane The foregroundPane
     */
    private LocationPane(final ForegroundPane foregroundPane) throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        super();

        this.foregroundPane = foregroundPane;

        tileMap = new HashMap<>();
        regionMap = new HashMap<>();
        zoneMap = new HashMap<>();
        connectionMap = new HashMap<>();
        debugNodes = new ArrayList<>();

        selectedLocationId = new SimpleStringProperty(Constants.DUMMY_LABEL);
        rootLocation = null;

        onEmptyAreaClickHandler = event -> {
            if (event.isStillSincePress() && rootLocation != null) {
                if (event.getClickCount() == 1) {
                    selectRootLocation();
                } else if (event.getClickCount() == 2) {
                    this.autoFocusPolygonAnimated(rootLocation);
                }

                try {
                    foregroundPane.getContextMenu().getRoomInfo().setText(selectedLocation.getLabel());
                } catch (NotAvailableException ex) {
                    LOGGER.warn("Could not resolve location label!", ex);
                }
            }
        };

        this.heightProperty().addListener((observable, oldValue, newValue)
                -> this.setTranslateY(this.getTranslateY() - ((oldValue.doubleValue() - newValue.doubleValue()) / 2) * this.getScaleY()));

        this.widthProperty().addListener((observable, oldValue, newValue)
                -> this.setTranslateX(this.getTranslateX() - ((oldValue.doubleValue() - newValue.doubleValue()) / 2) * this.getScaleX()));

        this.foregroundPane.getMainMenuWidthProperty().addListener((observable, oldValue, newValue)
                -> this.setTranslateX(this.getTranslateX() - ((oldValue.doubleValue() - newValue.doubleValue()) / 2)));
        
        onMouseMovedProperty().addListener((observable, oldValue, newValue) -> {
            LOGGER.info("mouse moved:");
            
        });
    }

    private void selectRootLocation() {

        // check if exists
        if (rootLocation == null) {
            LOGGER.debug("Could not select root because its not available.");
            return;
        }

        // check is not already selected
        if (rootLocation.equals(selectedLocation)) {
            return;
        }

        // delected selected location
        if (selectedLocation != null) {
            selectedLocation.setSelected(false);
        }
        rootLocation.setSelected(true);
        try {
            this.setSelectedLocation(rootLocation);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not select root loaction!", ex, LOGGER);
        }
    }

    /**
     * Singleton Pattern. This method call can not be used to instantiate the
     * singleton.
     *
     * @return the singleton instance of the location pane
     * @throws InstantiationException thrown if no getInstance(ForegroundPane
     * foregroundPane) is called before
     */
    public static LocationPane getInstance() throws InstantiationException {
        synchronized (LocationPane.class) {
            if (LocationPane.instance == null) {
                throw new InstantiationException();
            }
        }
        return LocationPane.instance;
    }

    public void setInitialized(boolean init) {
        initialized = init;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public ForegroundPane getForeground() {
        return this.foregroundPane;
    }

    /**
     * Singleton Pattern.
     *
     * @return the singleton instance of the location pane
     * @param foregroundPane the foreground pane needed for instantiation
     * @throws org.openbase.jul.exception.InstantiationException
     */
    public static LocationPane getInstance(final ForegroundPane foregroundPane) throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        synchronized (LocationPane.class) {
            if (LocationPane.instance == null) {
                LocationPane.instance = new LocationPane(foregroundPane);
            }
        }
        return LocationPane.instance;
    }

    /**
     * Adds a room to the location Pane and use the controls to add a mouse
     * event handler.
     *
     * If a room with the same id already exists, it will be overwritten.
     *
     * @param locationUnitConfig the configuration of the location to add.
     * @param vertices A list of vertices which defines the shape of the room
     * @throws org.openbase.jul.exception.CouldNotPerformException
     * @throws java.lang.InterruptedException
     */
    public void addLocation(final UnitConfig locationUnitConfig, final List<Point2D> vertices) throws CouldNotPerformException, InterruptedException {
        try {
            // Fill the list of vertices into an array of points
            double[] points = new double[vertices.size() * 2];
            for (int i = 0; i < vertices.size(); i++) {
                // TODO: X and Y are swapped in the world of the csra... make it more generic...
                points[i * 2] = vertices.get(i).getY() * Constants.METER_TO_PIXEL;
                points[i * 2 + 1] = vertices.get(i).getX() * Constants.METER_TO_PIXEL;
            }

            final LocationPolygon locationPolygon;

            switch (locationUnitConfig.getLocationConfig().getType()) {
                case TILE:
                    locationPolygon = new TilePolygon(points);
                    locationPolygon.init(locationUnitConfig);
                    locationPolygon.activate();
                    addMouseEventHandlerToTile((TilePolygon) locationPolygon);
                    tileMap.put(locationUnitConfig.getId(), (TilePolygon) locationPolygon);
                    break;
                case REGION:
                    locationPolygon = new RegionPolygon(points);
                    locationPolygon.init(locationUnitConfig);
                    locationPolygon.activate();
                    addMouseEventHandlerToRegion((RegionPolygon) locationPolygon);
                    regionMap.put(locationUnitConfig.getId(), (RegionPolygon) locationPolygon);
                    break;
                case ZONE:
                    locationPolygon = new ZonePolygon(points);
                    locationPolygon.init(locationUnitConfig);
                    locationPolygon.activate();
                    if (locationUnitConfig.getLocationConfig().getRoot()) {
                        rootLocation = (ZonePolygon) locationPolygon; //TODO: handle the situation where several zones exist
                    }
                    zoneMap.put(locationUnitConfig.getId(), (ZonePolygon) locationPolygon);
                    break;
                default:
                    throw new EnumNotSupportedException(locationUnitConfig.getLocationConfig().getType(), this);
            }

            // Paint debug informations
            if (JPService.debugMode()) {

                // declare vars
                Text text;
                Circle coordinate;
                final StackPane globalBaseStack = new StackPane();
                final StackPane locationBaseStack = new StackPane();
                final StackPane[] locationStacks = new StackPane[vertices.size()];
                debugNodes.clear();

                // Paint Location Coordinates
                final double COORDINATE_BLOCK_SIZE = 0.30 * Constants.METER_TO_PIXEL;
                for (int i = 0; i < vertices.size(); i++) {

                    text = new Text(Integer.toString(i));
                    text.setStroke(Color.BLACK);

                    coordinate = new Circle(COORDINATE_BLOCK_SIZE);
                    coordinate.setFill(Color.WHITE);
                    coordinate.setEffect(new Lighting());

                    locationStacks[i] = new StackPane();
                    locationStacks[i].getChildren().addAll(coordinate, text);
                    locationStacks[i].autosize();
                    locationStacks[i].setLayoutX(vertices.get(i).getY() * Constants.METER_TO_PIXEL - (locationStacks[i].getWidth() / 2));
                    locationStacks[i].setLayoutY(vertices.get(i).getX() * Constants.METER_TO_PIXEL - (locationStacks[i].getHeight() / 2));
                    final int pos = i;
                    locationStacks[i].hoverProperty().addListener((observable, oldValue, newValue) -> {
                        InfoPane.info("This is the " + pos + ". coordinate of the " + locationUnitConfig.getLabel());
                    });
                    debugNodes.add(locationStacks[i]);
                }

                // Paint LocationBase
                text = new Text("X");
                text.setStroke(Color.BLACK);

                coordinate = new Circle(COORDINATE_BLOCK_SIZE);
                coordinate.setFill(Color.CORNFLOWERBLUE);
                coordinate.setEffect(new Lighting());

                locationBaseStack.getChildren().addAll(coordinate, text);
                locationBaseStack.autosize();
                locationBaseStack.setLayoutX(locationUnitConfig.getPlacementConfig().getPosition().getTranslation().getY() * Constants.METER_TO_PIXEL - (locationBaseStack.getWidth() / 2));
                locationBaseStack.setLayoutY(locationUnitConfig.getPlacementConfig().getPosition().getTranslation().getX() * Constants.METER_TO_PIXEL - (locationBaseStack.getHeight() / 2));
                locationBaseStack.hoverProperty().addListener((observable, oldValue, newValue) -> {
                    InfoPane.info("This is the base of the " + locationUnitConfig.getLabel());
                });
                debugNodes.add(locationBaseStack);

                // Paint Gloabl Base
                text = new Text("O");
                text.setStroke(Color.BLACK);

                coordinate = new Circle(COORDINATE_BLOCK_SIZE);
                coordinate.setFill(Color.DARKRED);
                coordinate.setEffect(new Lighting());

                globalBaseStack.getChildren().addAll(coordinate, text);
                globalBaseStack.autosize();
                globalBaseStack.setLayoutX(0 - (globalBaseStack.getWidth() / 2));
                globalBaseStack.setLayoutY(0 - (globalBaseStack.getHeight() / 2));
                globalBaseStack.hoverProperty().addListener((observable, oldValue, newValue) -> {
                    InfoPane.info("This is the global base.");
                });
                debugNodes.add(globalBaseStack);
            }
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not add location!", ex);
        }
    }

    /**
     * Adds a connection to the location Pane.
     *
     * If a connection with the same id already exists, it will be overwritten.
     *
     * @param connectionUnitConfig the unit config of this connection.
     * @param vertices A list of vertices which defines the shape of the
     * connection
     * @throws org.openbase.jul.exception.CouldNotPerformException
     * @throws java.lang.InterruptedException
     */
    public void addConnection(final UnitConfig connectionUnitConfig, final List<Point2D> vertices) throws CouldNotPerformException, InterruptedException {

        try {
            // Fill the list of vertices into an array of points
            double[] points = new double[vertices.size() * 2];
            for (int i = 0; i < vertices.size(); i++) {
                // TODO: X and Y are swapped in the world of the csra... make it more generic...
                points[i * 2] = vertices.get(i).getY() * Constants.METER_TO_PIXEL;
                points[i * 2 + 1] = vertices.get(i).getX() * Constants.METER_TO_PIXEL;
            }

            ConnectionPolygon connectionPolygon;

            switch (connectionUnitConfig.getConnectionConfig().getType()) {
                case DOOR:
                    connectionPolygon = new DoorPolygon(points);
                    break;
                case WINDOW:
                    connectionPolygon = new WindowPolygon(points);
                    break;
                case PASSAGE:
                    connectionPolygon = new PassagePolygon(points);
                    break;
                default:
                    throw new EnumNotSupportedException(connectionUnitConfig.getConnectionConfig().getType(), this);
            }

            connectionPolygon.init(connectionUnitConfig);
            connectionPolygon.activate();

            connectionMap.put(connectionUnitConfig.getId(), connectionPolygon);

            connectionUnitConfig.getConnectionConfig().getTileIdList().forEach(locationId -> {
                if (tileMap.containsKey(locationId)) {
                    tileMap.get(locationId).addCuttingShape(connectionPolygon);
                } else {
                    String unitLabel = locationId;
                    try {
                        unitLabel = Registries.getUnitRegistry(false).getUnitConfigById(locationId).getLabel();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        // id is used instead.
                    }
                    
                    LOGGER.debug("Location " + unitLabel + " can not be found in the location Map. No Cutting will be applied.");
                }
            });
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not add connection!", ex);
        }
    }

    /**
     * Will add a UnitIcon to the locationPane.
     *
     * @param svgIcon The icon
     * @param onActionHandler The Handler that gets activated when the button is
     * pressed
     * @param position The position where the button is to be placed
     *
     * public void addUnit(final SVGIcon svgIcon, final EventHandler<ActionEvent> onActionHandler,
     * final Point2D position) {
     * final UnitButton unitButton = new UnitButton(svgIcon, onActionHandler);
     * unitButton.setTranslateX(position.getX());
     * unitButton.setTranslateY(position.getY());
     * this.getChildren().add(unitButton);
     * } *
     * public void addUnit(final SVGIcon svgIcon,
     * final Point2D position) {
     * final UnitButton unitButton = new UnitButton(svgIcon, null);
     * unitButton.setTranslateX(position.getX());
     * unitButton.setTranslateY(position.getY());
     * //unitSymbols.add(unitButton);
     * }
     */
    /**
     * Erases all locations from the locationPane.
     */
    public void clearLocations() {
        tileMap.forEach((locationId, locationPolygon) -> {
            locationPolygon.shutdown();
            this.getChildren().remove(locationPolygon);
        }
        );
        tileMap.clear();

        regionMap.forEach((locationId, locationPolygon) -> {
            locationPolygon.shutdown();
            this.getChildren().remove(locationPolygon);
        }
        );
        regionMap.clear();

        rootLocation = null;
    }

    /**
     * Erases all connections from the locationPane.
     */
    public void clearConnections() {
        connectionMap.forEach((connectionId, connectionPolygon) -> {
            connectionPolygon.shutdown();
            this.getChildren().remove(connectionPolygon);
        }
        );
        connectionMap.clear();
    }

    /**
     * Will clear everything on the location Pane and then add everything that
     * is saved in the maps. Also adds a cutting shape for every Polygon to the
     * root.
     */
    public void updateLocationPane() {
        this.getChildren().clear();

        tileMap.forEach((locationId, locationPolygon) -> {
            if (rootLocation != null) {
                rootLocation.addCuttingShape(locationPolygon);
            }
            this.getChildren().add(locationPolygon);
        });

        regionMap.forEach((locationId, locationPolygon) -> {
            if (rootLocation != null) {
                rootLocation.addCuttingShape(locationPolygon);
            }
            this.getChildren().add(locationPolygon);
        });

        connectionMap.forEach((connectionId, connectionPolygon) -> {
            if (rootLocation != null) {
                rootLocation.addCuttingShape(connectionPolygon);
            }
            this.getChildren().add(connectionPolygon);
        });

        if (rootLocation != null) {
            this.getChildren().add(rootLocation);
        }

        if (JPService.debugMode()) {
            // debug print
            for (final Node debugNode : debugNodes) {
                this.getChildren().add(debugNode);
            }
        }

        if (!isLocationSelected()) {
            selectRootLocation();
        }
    }

    private boolean isLocationSelected() {
        return selectedLocation != null;
    }

    /**
     * Adds a mouse eventHandler to the tile.
     *
     * @param tile The tile
     */
    public void addMouseEventHandlerToTile(final TilePolygon tile) {
        tile.setOnMouseClicked(event -> {
            try {
                event.consume();

                if (event.isStillSincePress()) {
                    if (event.getClickCount() == 1) {
                        this.setSelectedLocation(tile);
                        this.lastFirstClickTarget = tile;
                    } else if (event.getClickCount() == 2) {
                        autoFocusPolygonAnimated(tile);
                    }
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not handle mouse event!", ex, LOGGER);
            }
        });
        tile.setOnMouseEntered(event -> {
            event.consume();
            tile.mouseEntered();
            try {
                InfoPane.info(tile.getLabel());
            } catch (final NotAvailableException ex) {
                LOGGER.warn("Could not resolve location label!", ex);
            }
        });
        tile.setOnMouseExited(event -> {
            event.consume();
            tile.mouseLeft();
            InfoPane.info("");
        });
    }

    /**
     * Adds a mouse eventHandler to the region.
     *
     * @param region The region
     */
    public void addMouseEventHandlerToRegion(final RegionPolygon region) {
        region.setOnMouseClicked(event -> {
            try {
                event.consume();

                if (event.isStillSincePress()) {
                    if (event.getClickCount() == 1) {
                        this.setSelectedLocation(region);
                        this.lastFirstClickTarget = region;
                    } else if (event.getClickCount() == 2) {
                        if (this.lastFirstClickTarget.equals(region)) {
                            autoFocusPolygonAnimated(region);
                        } else {
                            selectedLocation.fireEvent(event.copyFor(null, selectedLocation));
                        }
                    }

                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not handle mouse event!", ex, LOGGER);
            }
        });
        region.setOnMouseEntered(event -> {
            try {
                event.consume();
                region.mouseEntered();
                InfoPane.info(region.getLabel());
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not handle mouse event!", ex, LOGGER);
            }
        });
        region.setOnMouseExited(event -> {
            event.consume();
            region.mouseLeft();
            InfoPane.info("");
        });
    }

    private void setSelectedLocation(final LocationPolygon newSelectedLocation) throws CouldNotPerformException {
        try {
            if (selectedLocation != null && selectedLocation.equals(newSelectedLocation)) {
                // already selected
                return;
            }

            if (lastSelectedTile != null) {
                // make sub sub regions unselectable
                if (!newSelectedLocation.getClass().equals(RegionPolygon.class)) {
                    lastSelectedTile.getChildIds().forEach(childId -> {
                        try {
                            // make all regions non selecable
                            if (regionMap.containsKey(childId)) {
                                regionMap.get(childId).changeStyleOnSelectable(false);
                            }
                        } catch (Exception ex) {
                            ExceptionPrinter.printHistory(ex, LOGGER);
                        }
                    });
                }
            }

            // allow selection of sub regions.
            if (newSelectedLocation.getClass().equals(TilePolygon.class)) {
                lastSelectedTile = newSelectedLocation;
                newSelectedLocation.getChildIds().forEach(childId -> {
                    try {
                        regionMap.get(childId).changeStyleOnSelectable(true);
                    } catch (Exception ex) {
                        ExceptionPrinter.printHistory(ex, LOGGER);
                    }
                });
            }

            if (selectedLocation != null) {
                selectedLocation.setSelected(false);
            }
            newSelectedLocation.setSelected(true);
            selectedLocation = newSelectedLocation;
            selectedLocationId.set(newSelectedLocation.getUnitId());

            foregroundPane.getContextMenu().getRoomInfo().setText(selectedLocation.getLabel());

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not select location polygon!", ex);
        }
    }

    /**
     * ZoomFits to the root if available. Otherwise to the first location in the
     * tileMap.
     */
    public void zoomFit() {
        if (rootLocation != null) { //NOPMD
            autoFocusPolygon(rootLocation);
        } else if (!tileMap.isEmpty()) {
            autoFocusPolygon(tileMap.values().iterator().next());
        }
    }

    /**
     * Adds a change listener to the selectedRoomID property.
     *
     * @param changeListener The change Listener
     */
    public void addSelectedLocationIdListener(final ChangeListener<? super String> changeListener) {
        selectedLocationId.addListener(changeListener);
    }

    /**
     * Remove the specified change listener from the selectedRoomID property.
     *
     * @param changeListener The change Listener
     */
    public void removeSelectedLocationIdListener(final ChangeListener<? super String> changeListener) {
        selectedLocationId.removeListener(changeListener);
    }

    /**
     * Getter for the OnEmptyAreaClickHandler.
     *
     * @return The EventHandler.
     */
    public EventHandler<MouseEvent> getOnEmptyAreaClickHandler() {
        return onEmptyAreaClickHandler;
    }
    
    private void autoFocusPolygon(final LocationPolygon polygon) {
        final double xScale = (foregroundPane.getBoundingBox().getWidth() / polygon.prefWidth(0))
                * Constants.ZOOM_FIT_PERCENTAGE_WIDTH;
        final double yScale = (foregroundPane.getBoundingBox().getHeight() / polygon.prefHeight(0))
                * Constants.ZOOM_FIT_PERCENTAGE_HEIGHT;
        final double scale = (xScale < yScale) ? xScale : yScale;

        this.setScaleX(scale);
        this.setScaleY(scale);

        final Point2D transition = calculateTransition(scale, polygon);

        this.setTranslateX(transition.getX());
        this.setTranslateY(transition.getY());
    }

    private void autoFocusPolygonAnimated(final LocationPolygon polygon) {
        final double xScale = (foregroundPane.getBoundingBox().getWidth() / polygon.prefWidth(0))
                * Constants.ZOOM_FIT_PERCENTAGE_WIDTH;
        final double yScale = (foregroundPane.getBoundingBox().getHeight() / polygon.prefHeight(0))
                * Constants.ZOOM_FIT_PERCENTAGE_HEIGHT;
        final double scale = (xScale < yScale) ? xScale : yScale;

        final ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(500));
        scaleTransition.setToX(scale);
        scaleTransition.setToY(scale);
        scaleTransition.setCycleCount(1);
        scaleTransition.setAutoReverse(true);

        final Point2D transition = calculateTransition(scale, polygon);

        final TranslateTransition translateTransition = new TranslateTransition(Duration.millis(500));
        translateTransition.setToX(transition.getX());
        translateTransition.setToY(transition.getY());
        translateTransition.setCycleCount(1);
        translateTransition.setAutoReverse(true);

        final ParallelTransition parallelTransition
                = new ParallelTransition(this, scaleTransition, translateTransition);
        parallelTransition.play();
    }

    private Point2D calculateTransition(final double scale, final LocationPolygon polygon) {
        final double polygonDistanceToCenterX = (-(polygon.getCenterX() - (getLayoutBounds().getWidth() / 2))) * scale;
        final double polygonDistanceToCenterY = (-(polygon.getCenterY() - (getLayoutBounds().getHeight() / 2))) * scale;
        final double boundingBoxCenterX
                = (foregroundPane.getBoundingBox().getMinX() + foregroundPane.getBoundingBox().getMaxX()) / 2;
        final double boundingBoxCenterY
                = (foregroundPane.getBoundingBox().getMinY() + foregroundPane.getBoundingBox().getMaxY()) / 2;
        final double bbCenterDistanceToCenterX = ((getLayoutBounds().getWidth() / 2) - boundingBoxCenterX);
        final double bbCenterDistanceToCenterY = ((getLayoutBounds().getHeight() / 2) - boundingBoxCenterY);
        final double transitionX = polygonDistanceToCenterX - bbCenterDistanceToCenterX;
        final double transitionY = polygonDistanceToCenterY - bbCenterDistanceToCenterY;

        return new Point2D(transitionX, transitionY);
    }
}
