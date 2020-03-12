/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Executes the conversion of CSV files to WKT.
 *
 * The {@link #DIRECTORY} variable should be set before to execute this class.
 */
public final class Main {
    /**
     * Defines here the home directory where are located files.
     */
    private static final String DIRECTORY = "/path/to/my/directory";

    public static void main(String[] args) throws Exception {
        System.setErr(System.out);

        final Path dir = Paths.get(DIRECTORY);
        final EllipsoidParser ellipsoids = new EllipsoidParser();
        ellipsoids.read(dir.resolve("ellipsoid.csv"));

        final DatumParser datums = new DatumParser(ellipsoids);
        datums.read(dir.resolve("datum.csv"));

        final PlanetodeticParser planetodetics = new PlanetodeticParser(datums);
        planetodetics.read(dir.resolve("planetodetic.csv"));

        final ProjectedParser projected = new ProjectedParser(planetodetics);
        projected.read(dir.resolve("projection.csv"));

        Parser.save(dir.resolve("wkt.txt"), planetodetics, projected);
    }
}
