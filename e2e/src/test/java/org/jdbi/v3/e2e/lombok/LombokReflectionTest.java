/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.e2e.lombok;

import java.util.Collections;
import java.util.List;

import jakarta.annotation.Nullable;

import lombok.Data;
import lombok.Value;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.Nested;
import org.jdbi.v3.core.mapper.PropagateNull;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.JdbiH2Extension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is the test case to reproduce #2085. Contributed by @DudkoMatt
 * <p>
 * If you look at this class to figure out how to use JDBI with lombok, please be aware that there is a lombok.config file right next to this source file which
 * is needed to propagate the Nested and PropagateNull annotations onto the autogenerated classes. Without this config file, the test will fail (and so will
 * your code).
 */
public class LombokReflectionTest {

    private static final String QUERY = "SELECT\n"
        + "b.id AS b_id,\n"
        + "b.some_column AS b_s,\n"
        + "c.id AS c_id,\n"
        + "c.additional_column AS c_additionalColumn\n"
        + "FROM table_b as b\n"
        + "LEFT JOIN table_c AS c\n"
        + "ON b.id = c.id\n"
        + "WHERE b.id = :id\n";

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiH2Extension.h2().withInitializer((ds, h) -> {
        h.execute("CREATE TABLE table_b (id serial primary key, some_column varchar)");
        h.execute("CREATE TABLE table_c (id serial primary key, additional_column varchar)");

        h.createUpdate("INSERT INTO table_b VALUES(:id, :some_column)").bind("id", 1).bind("some_column", "first").execute();
        h.createUpdate("INSERT INTO table_b VALUES(:id, :some_column)").bind("id", 2).bind("some_column", "second").execute();
        h.createUpdate("INSERT INTO table_c VALUES(:id, :additional)").bind("id", 1).bind("additional", "additional").execute();
    });

    @Test
    public void testLombokValue() {
        Handle handle = h2Extension.getSharedHandle();

        final long id1 = 1L;
        final long id2 = 2L;

        List<ValueA> expectedById1 = Collections.singletonList(new ValueA(new ValueB(id1, "first"), new ValueC(id1, "additional")));
        List<ValueA> actualById1 = handle.createQuery(QUERY).bind("id", id1).map(ConstructorMapper.of(ValueA.class)).list();
        assertThat(actualById1).containsExactlyElementsOf(expectedById1);

        List<ValueA> expectedById2 = Collections.singletonList(new ValueA(new ValueB(id2, "second"), null));
        List<ValueA> actualById2 = handle.createQuery(QUERY).bind("id", id2).map(ConstructorMapper.of(ValueA.class)).list();
        assertThat(actualById2).containsExactlyElementsOf(expectedById2);
    }

    @Value
    public static class ValueA {

        @Nested(value = "b")
        ValueB b;

        @Nullable
        @Nested(value = "c")
        ValueC c;
    }

    @Value
    public static class ValueB {

        Long id;
        String s;
    }

    @Value
    @PropagateNull(value = "id")
    public static class ValueC {

        Long id;
        String additionalColumn;
    }

    @Test
    public void testLombokData() {
        Handle handle = h2Extension.getSharedHandle();

        final long id1 = 1L;
        final long id2 = 2L;

        DataC id1C = new DataC();
        id1C.setId(id1);
        id1C.setAdditionalColumn("additional");

        DataB id1B = new DataB();
        id1B.setId(id1);
        id1B.setS("first");

        DataA id1A = new DataA();
        id1A.setB(id1B);
        id1A.setC(id1C);

        List<DataA> expectedById1 = Collections.singletonList(id1A);
        List<DataA> actualById1 = handle.createQuery(QUERY).bind("id", id1).map(BeanMapper.of(DataA.class)).list();
        assertThat(actualById1).containsExactlyElementsOf(expectedById1);

        DataB id2B = new DataB();
        id2B.setId(id2);
        id2B.setS("second");

        DataA id2A = new DataA();
        id2A.setB(id2B);

        List<DataA> expectedById2 = Collections.singletonList(id2A);
        List<DataA> actualById2 = handle.createQuery(QUERY).bind("id", id2).map(BeanMapper.of(DataA.class)).list();
        assertThat(actualById2).containsExactlyElementsOf(expectedById2);
    }

    @Data
    public static class DataA {

        @Nested(value = "b")
        DataB b;

        @Nullable
        @Nested(value = "c")
        DataC c;
    }

    @Data
    public static class DataB {

        Long id;
        String s;
    }

    @Data
    @PropagateNull(value = "id")
    public static class DataC {

        Long id;
        String additionalColumn;
    }
}