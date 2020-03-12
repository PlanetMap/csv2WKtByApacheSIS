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

import org.apache.sis.measure.Units;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.GeodeticCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.Conversion;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;


/**
 * Parses the {@code "projected.csv"} file.
 */
final class ProjectedParser extends Parser<ProjectedCRS> {
    private final PlanetodeticParser planetodetics;

    private final CartesianCS cartesianEastOriented, cartesianWestOriented;

    ProjectedParser(final PlanetodeticParser planetodetics) throws FactoryException {
        this.planetodetics = planetodetics;
        final CSFactory csFactory = getCSFactory();
        CoordinateSystemAxis y = csFactory.createCoordinateSystemAxis(name("Northing"), "N", AxisDirection.NORTH, Units.METRE);
        CoordinateSystemAxis x = csFactory.createCoordinateSystemAxis(name("Easting"),  "E", AxisDirection.EAST,  Units.METRE);
        cartesianEastOriented = csFactory.createCartesianCS(name("Planetary Cartesian CS (East oriented)"), x, y);

        x = csFactory.createCoordinateSystemAxis(name("Westing"), "W", AxisDirection.WEST, Units.METRE);
        cartesianWestOriented = csFactory.createCartesianCS(name("Planetary Cartesian CS (West oriented)"), x, y);
    }

    /*
     * Columns order in CSV file:
     *   [0]: authority
     *   [1]: version
     *   [2]: code
     *   [3]: name
     *   [4]: baseCRS
     *   [5]: method
     *   [6]: parameterName
     *   [7]: parameterValue
     *        (repeated as necessary)
     */
    @Override
    protected ProjectedCRS create(final String[] columns) throws FactoryException {
        final GeodeticCRS baseCRS = planetodetics.get(columns[4]);
        if (baseCRS instanceof GeographicCRS) try {
            final AxisDirection direction = baseCRS.getCoordinateSystem().getAxis(1).getDirection();
            final CartesianCS cs;
            if (direction == AxisDirection.WEST) cs = cartesianWestOriented;
            else if (direction == AxisDirection.EAST) cs = cartesianEastOriented;
            else throw fail("Unexpected axis direction: " + direction);

            final CoordinateOperationFactory opFactory = getCoordinateOperationFactory();
            final OperationMethod method = getCoordinateOperationFactory().getOperationMethod(columns[5]);
            final ParameterValueGroup pg = method.getParameters().createValue();
            for (int i=6; i<columns.length; i+=2) {
                String name = columns[i];
                switch (name) {
                    case "Longitude_Of_Center": name = "Central_Meridian"; break;   // For Sinusoidal.
                }
                if (!name.isEmpty()) {
                    pg.parameter(name).setValue(parseDouble(columns, i+1));
                }
            }
            final Conversion conversionFromBase = opFactory.createDefiningConversion(name(columns[3]), method, pg);
            return getCRSFactory().createProjectedCRS(properties(columns), (GeographicCRS) baseCRS, conversionFromBase, cs);
            // TODO: needs GeoAPI and SIS update for allowing geocentric base CRS.
        } catch (NoSuchIdentifierException e) {
            System.out.println(e);
        }
        return null;
   }
}
