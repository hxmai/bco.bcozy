/**
 * ==================================================================
 *
 * This file is part of org.dc.bco.bcozy.
 *
 * org.dc.bco.bcozy is free software: you can redistribute it and modify
 * it under the terms of the GNU General Public License (Version 3)
 * as published by the Free Software Foundation.
 *
 * org.dc.bco.bcozy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with org.dc.bco.bcozy. If not, see <http://www.gnu.org/licenses/>.
 * ==================================================================
 */

package org.dc.bco.bcozy.controller;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import javafx.application.Platform;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import org.dc.bco.bcozy.view.ForegroundPane;
import org.dc.bco.bcozy.view.location.LocationPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rct.Transform;
import rct.TransformerException;
import rst.math.Vec3DDoubleType;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationRegistryType;

import javax.vecmath.Point3d;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class LocationController implements Observer<LocationRegistryType.LocationRegistry> {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationController.class);

    private final ForegroundPane foregroundPane;
    private final LocationPane locationPane;
    private final RemotePool remotePool;
    private LocationRegistryRemote locationRegistryRemote;

//    private final Map<String, LocationPolygon> locationPolygonMap;

    /**
     * The constructor.
     *
     * @param foregroundPane the foreground pane
     * @param locationPane the location pane
     * @param remotePool the remotePool
     *
     * @throws InstantiationException This exception will be thrown if no LocationRegistryRemote could be instantiated
     */
    public LocationController(final ForegroundPane foregroundPane, final LocationPane locationPane,
                              final RemotePool remotePool) throws InstantiationException {
        this.foregroundPane = foregroundPane;
        this.locationPane = locationPane;
        this.remotePool = remotePool;

        this.foregroundPane.getMainMenu().addFetchLocationButtonEventHandler(event -> connectLocationRemote());
        //@Julian: This is the size of the bounding box within which the drawing should be done
        final BoundingBox boundingBox = foregroundPane.getBoundingBox();
        LOGGER.info("Height:" + boundingBox.getHeight());
        LOGGER.info("Width:" + boundingBox.getWidth());
        LOGGER.info("Min X:" + boundingBox.getMinX());
        LOGGER.info("Min Y:" + boundingBox.getMinY());
        LOGGER.info("Max X:" + boundingBox.getMaxX());
        LOGGER.info("Max Y:" + boundingBox.getMaxY());
    }

    /**
     * Establishes the connection with the RemoteRegistry.
     */
    public void connectLocationRemote() {
        if (remotePool.isInit()) {
            try {
                locationRegistryRemote = remotePool.getLocationRegistryRemote();
                locationRegistryRemote.addObserver(this);
                this.fetchLocation();

            } catch (CouldNotPerformException e) {
                ExceptionPrinter.printHistory(e, LOGGER, LogLevel.ERROR);
            }
        } else {
            LOGGER.warn("Registry Remotes are not initialized. Thus a Dummy Location will be loaded.");
            this.fetchDummyLocation();
        }
    }

    private void fetchLocation() throws CouldNotPerformException {
        final List<LocationConfigType.LocationConfig> list = locationRegistryRemote.getLocationConfigs();

        locationPane.clearLocations();

        //search for root
        String rootId = "";

        for (final LocationConfigType.LocationConfig locationConfig : list) {
            if (locationConfig.getRoot()) {
                rootId = locationConfig.getId();
            }
        }

        //check which location have a shape
        for (final LocationConfigType.LocationConfig locationConfig : list) {
            if (locationConfig.getPlacementConfig().hasShape()) {
                try {
                    final List<Point2D> vertices = new LinkedList<>();

                    // Get the transformation for the current room
                    final Transform transform =
                            remotePool.getTransformReceiver()
                                    .lookupTransform(rootId, locationConfig.getId(), System.currentTimeMillis());

                    // Get the shape of the room
                    final List<Vec3DDoubleType.Vec3DDouble> shape =
                            locationConfig.getPlacementConfig().getShape().getFloorList();

                    // Iterate over all vertices
                    for (final Vec3DDoubleType.Vec3DDouble rstVertex : shape) {
                        // Convert vertex into java type
                        final Point3d vertex = new Point3d(rstVertex.getX(), rstVertex.getY(), rstVertex.getZ());
                        // Transform
                        transform.getTransform().transform(vertex);
                        // Add vertex to list of vertices
                        vertices.add(new Point2D(vertex.x, vertex.y));
                    }

                    locationPane.addRoom(locationConfig.getId(), locationConfig.getLabel(), vertices,
                            locationConfig.getType().toString());
                } catch (TransformerException e) {
                    ExceptionPrinter.printHistory(e, LOGGER, LogLevel.ERROR);
                    LOGGER.warn("Could not gather transformation for room: " + locationConfig.getId());
                } catch (CouldNotPerformException e) {
                    ExceptionPrinter.printHistory(e, LOGGER, LogLevel.ERROR);
                    LOGGER.warn("TransformReceiver was not properly initialized.");
                }
            }
        }

        locationPane.zoomFit();
    }

    private void fetchDummyLocation() {
        locationPane.clearLocations();

        //CHECKSTYLE.OFF: MagicNumber
        final List<Point2D> zoneVertices = new LinkedList<>();
        zoneVertices.add(new Point2D(0, 0));
        zoneVertices.add(new Point2D(10, 0));
        zoneVertices.add(new Point2D(10, 10));
        zoneVertices.add(new Point2D(0, 10));
        locationPane.addRoom("DummyID0", "DummyLabel0", zoneVertices,
                LocationConfigType.LocationConfig.LocationType.ZONE.toString());

        final List<Point2D> tile0Vertices = new LinkedList<>();
        tile0Vertices.add(new Point2D(1, 1));
        tile0Vertices.add(new Point2D(5, 1));
        tile0Vertices.add(new Point2D(5, 3));
        tile0Vertices.add(new Point2D(1, 3));
        locationPane.addRoom("DummyID1", "DummyLabel1", tile0Vertices,
                LocationConfigType.LocationConfig.LocationType.TILE.toString());

        final List<Point2D> tile1Vertices = new LinkedList<>();
        tile1Vertices.add(new Point2D(6, 1));
        tile1Vertices.add(new Point2D(6, 8));
        tile1Vertices.add(new Point2D(8, 8));
        tile1Vertices.add(new Point2D(8, 1));
        locationPane.addRoom("DummyID2", "DummyLabel2", tile1Vertices,
                LocationConfigType.LocationConfig.LocationType.TILE.toString());
        //CHECKSTYLE.ON: MagicNumber

        locationPane.zoomFit();
    }

    @Override
    public void update(final Observable<LocationRegistryType.LocationRegistry> observable,
                       final LocationRegistryType.LocationRegistry locationRegistry) throws Exception { //NOPMD
        Platform.runLater(() -> {
            try {
                fetchLocation();
            } catch (CouldNotPerformException e) {
                ExceptionPrinter.printHistory(e, LOGGER, LogLevel.ERROR);
            }
        });

    }
}
