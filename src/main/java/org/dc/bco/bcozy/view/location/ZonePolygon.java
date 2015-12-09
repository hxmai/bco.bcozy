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

package org.dc.bco.bcozy.view.location;

/**
 *
 */
public class ZonePolygon extends LocationPolygon {

    /**
     * The Constructor for a ZonePolygon.
     *
     * @param label The label of the location
     * @param id The id of the location
     * @param points The vertices of the location
     */
    public ZonePolygon(final String label, final String id, final double... points) {
        super(label, id, LocationType.ZONE, points);
    }
}