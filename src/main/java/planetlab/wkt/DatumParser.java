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
import org.opengis.referencing.datum.DatumFactory;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.util.FactoryException;


/**
 * Parses the {@code "datum.csv"} file.
 */
final class DatumParser extends Parser<GeodeticDatum> {
    private final EllipsoidParser ellipsoids;

    DatumParser(final EllipsoidParser ellipsoids) {
        this.ellipsoids = ellipsoids;
    }

    /*
     * Columns order in CSV file:
     *   [0]: authority
     *   [1]: version
     *   [2]: code
     *   [3]: name
     *   [4]: body
     *   [5]: ellipsoid
     *   [6]: primeMeridianName
     *   [7]: primeMeridianValue
     */
    @Override
    protected GeodeticDatum create(final String[] columns) throws FactoryException {
        final Ellipsoid ellipsoid = ellipsoids.get(columns[5]);
        if (ellipsoid != null) {
            final DatumFactory datumFactory = getDatumFactory();
            final Map<String,?> properties = properties(columns);
            final double primeMeridianValue = parseDouble(columns, 7);
            if (primeMeridianValue > 0) {
                System.err.println("Non-zero prime meridian not yet supported.");
            } else {
                return datumFactory.createGeodeticDatum(properties, ellipsoid,
                       datumFactory.createPrimeMeridian(name(columns[6]), primeMeridianValue, Units.DEGREE));
            }
        }
        return null;
   }
}
