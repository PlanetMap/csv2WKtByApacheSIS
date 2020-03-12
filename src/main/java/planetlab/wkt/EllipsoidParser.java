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
import org.opengis.util.FactoryException;


/**
 * Parses the {@code "ellipsoid.csv"} file.
 */
final class EllipsoidParser extends Parser<Ellipsoid> {
    EllipsoidParser() {
    }

    /*
     * Columns order in CSV file:
     *   [0]: authority
     *   [1]: version
     *   [2]: code
     *   [3]: name
     *   [4]: semiMajorAxis
     *   [5]: semiMedianAxis
     *   [6]: semiMinorAxis
     *   [7]: inverseFlatenning
     */
    @Override
    protected Ellipsoid create(final String[] columns) throws FactoryException {
        final DatumFactory datumFactory = getDatumFactory();
        final double semiMajorAxis     = parseDouble(columns, 4);
        final double semiMedianAxis    = parseDouble(columns, 5);
        final double semiMinorAxis     = parseDouble(columns, 6);
        final double inverseFlattening = parseDouble(columns, 7);
        final Map<String,?> properties = properties(columns);
        if (semiMedianAxis > 0) {
            System.err.println("Triaxal ellipsoid not yet supported.");
            return null;
        } else if (inverseFlattening > 0) {
            return datumFactory.createFlattenedSphere(properties, semiMajorAxis, inverseFlattening, Units.METRE);
        } else {
            return datumFactory.createEllipsoid(properties, semiMajorAxis, semiMinorAxis, Units.METRE);
        }
   }
}
