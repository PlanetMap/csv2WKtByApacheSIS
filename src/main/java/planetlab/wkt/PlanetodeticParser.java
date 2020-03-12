/*
 * Licensed under the Apache License, Version 2.0 (the "License").
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. You may not use this
 * file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package planetlab.wkt;

import java.util.Map;
import org.apache.sis.measure.Units;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.util.FactoryException;


/**
 * Parses the {@code "planetodetic.csv"} file.
 */
final class PlanetodeticParser extends Parser<GeodeticCRS> {
    private final DatumParser datums;

    private final EllipsoidalCS ellipsoidalEastOriented, ellipsoidalWestOriented;

    private final SphericalCS sphericalEastOriented, sphericalWestOriented;

    PlanetodeticParser(final DatumParser datums) throws FactoryException {
        this.datums = datums;
        final CSFactory csFactory = getCSFactory();
        CoordinateSystemAxis y = csFactory.createCoordinateSystemAxis(name("Planetodetic latitude"),  "φ", AxisDirection.NORTH, Units.DEGREE);
        CoordinateSystemAxis x = csFactory.createCoordinateSystemAxis(name("Planetodetic longitude"), "λ", AxisDirection.EAST,  Units.DEGREE);
        ellipsoidalEastOriented = csFactory.createEllipsoidalCS(name("Planetary ellipsoidal CS (East oriented)"), y, x);

        x = csFactory.createCoordinateSystemAxis(name("Planetodetic longitude"), "λ", AxisDirection.WEST,  Units.DEGREE);
        ellipsoidalWestOriented = csFactory.createEllipsoidalCS(name("Planetary ellipsoidal CS (West oriented)"), y, x);

        y = csFactory.createCoordinateSystemAxis(name("Planetocentric latitude"),  "Ω", AxisDirection.NORTH, Units.DEGREE);
        x = csFactory.createCoordinateSystemAxis(name("Planetocentric longitude"), "θ", AxisDirection.EAST,  Units.DEGREE);
        CoordinateSystemAxis radius = csFactory.createCoordinateSystemAxis(name("Radius"), "R", AxisDirection.UP, Units.METRE);
        sphericalEastOriented = csFactory.createSphericalCS(name("Planetary spherical CS (East oriented)"), y, x, radius);

        x = csFactory.createCoordinateSystemAxis(name("Planetocentric longitude"), "θ", AxisDirection.WEST,  Units.DEGREE);
        sphericalWestOriented = csFactory.createSphericalCS(name("Planetary spherical CS (West oriented)"), y, x, radius);
    }

    /*
     * Columns order in CSV file:
     *   [0]: authority
     *   [1]: version
     *   [2]: code
     *   [3]: name
     *   [4]: datum
     *   [5]: csType
     *   [6]: longitudeDirection
     */
    @Override
    protected GeodeticCRS create(final String[] columns) throws FactoryException {
        final GeodeticDatum datum = datums.get(columns[4]);
        if (datum != null) {
            final boolean westOriented;
            switch (columns[6].toLowerCase()) {
                case "west": westOriented = true; break;
                case "east": westOriented = false; break;
                default: throw fail("Unrecognized axis direction: " + columns[6]);
            }
            final Map<String,?> properties = properties(columns);
            final CRSFactory crsFactory = getCRSFactory();
            switch (columns[5].toLowerCase()) {
                case "spherical": {
                    final SphericalCS cs = westOriented ? sphericalWestOriented : sphericalEastOriented;
                    return crsFactory.createGeocentricCRS(properties, datum, cs);
                }
                case "ellipsoidal": {
                    final EllipsoidalCS cs = westOriented ? ellipsoidalWestOriented : ellipsoidalEastOriented;
                    return crsFactory.createGeographicCRS(properties, datum, cs);
                }
                default: {
                    throw fail("Unrecognized coordinate system type: " + columns[5]);
                }
            }
        }
        return null;
   }
}
