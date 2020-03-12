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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.sis.internal.referencing.ReferencingFactoryContainer;
import org.apache.sis.internal.util.CollectionsExt;
import org.apache.sis.io.wkt.WKTFormat;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.collection.BackingStoreException;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.util.FactoryException;


/**
 * Base class of CSV file parser. Each subclass parses one kind of file and produce
 * one kind of object (ellipsoid, <i>etc.</i>).
 *
 * @param <T>
 */
abstract class Parser<T extends IdentifiedObject> extends ReferencingFactoryContainer implements Consumer<String> {
    private final Map<String,Object> properties;

    private final Map<String,T> results;

    Parser() {
        properties = new HashMap<>();
        results = new LinkedHashMap<>();
    }

    static Map<String,?> name(final String name) {
        return Collections.singletonMap(IdentifiedObject.NAME_KEY, name);
    }

    /*
     * Columns order in CSV file:
     *   [0]: authority
     *   [1]: version
     *   [2]: code
     *   [3]: name
     */
    final Map<String,?> properties(final String[] columns) {
        properties.clear();
        properties.put(IdentifiedObject.IDENTIFIERS_KEY, new ImmutableIdentifier(null, columns[0], columns[2], columns[1], null));
        properties.put(IdentifiedObject.NAME_KEY, columns[3]);
        return properties;
    }

    public final void read(final Path file) throws IOException, FactoryException {
        try (Stream<String> lines = Files.lines(file)) {
            lines.skip(1).forEach(this);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } catch (BackingStoreException e) {
            throw e.unwrapOrRethrow(FactoryException.class);
        }
    }

    @Override
    public final void accept(final String line) {
        if (!line.isEmpty() && line.charAt(0) != '#') {
            String[] columns = (String[]) CharSequences.split(line, ',');
            for (int i=columns.length; --i>=0;) {
                if (columns[i].endsWith("\"")) {
                    final StringBuilder buffer = new StringBuilder(columns[i]);
                    buffer.setLength(buffer.length() - 1);  // Remove last quote.
                    final int end = i;
                    do buffer.insert(0, ", ").insert(0, columns[--i]);
                    while (buffer.charAt(0) != '"');
                    buffer.deleteCharAt(0);
                    columns[i] = buffer.toString();
                    columns = ArraysExt.remove(columns, i+1, end - i);
                }
            }
            final T object;
            try {
                object = create(columns);
            } catch (FactoryException e) {
                throw new BackingStoreException(e);
            }
            if (object != null) {
                final ReferenceIdentifier id = CollectionsExt.first(object.getIdentifiers());
                final String key = nonNull(id.getCodeSpace()) + ':' + nonNull(id.getVersion()) + ':' + nonNull(id.getCode());
                if (results.putIfAbsent(key, object) != null) {
                    System.err.println("Duplicated entry: " + id);
                }
            }
        }
    }

    private static String nonNull(final String value) {
        return (value != null) ? value : "";
    }

    protected abstract T create(final String[] columns) throws FactoryException;

    static double parseDouble(final String[] columns, final int index) {
        if (index < columns.length) {
            final String value = columns[index];
            if (!value.isEmpty()) {
                return Double.parseDouble(value);
            }
        }
        return Double.NaN;
    }

    final T get(final String code) {
        return results.get(code);
    }

    static void save(final Path file, final Parser<?>... parsers) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(file)) {
            final WKTFormat f = new WKTFormat(null, null);
            for (final Parser<?> parser : parsers) {
                for (Object crs : parser.results.values()) {
                    if (crs != null) {
                        out.write(f.format(crs));
                        out.newLine();
                        out.newLine();
                    }
                }
            }
        }
    }

    static RuntimeException fail(String message) {
        throw new RuntimeException(message);
    }
}
