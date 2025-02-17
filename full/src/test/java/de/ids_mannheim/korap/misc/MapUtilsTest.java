package de.ids_mannheim.korap.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Test;
import de.ids_mannheim.korap.web.utils.MapUtils;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

@DisplayName("Map Utils Test")
class MapUtilsTest {

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Test Convert To Map")
    void testConvertToMap() {
        MultivaluedMap<String, String> mm = new MultivaluedHashMap<String, String>();
        mm.put("k1", Arrays.asList(new String[]{"a", "b", "c"}));
        mm.put("k2", Arrays.asList(new String[]{"d", "e", "f"}));
        Map<String, String> map = MapUtils.toMap(mm);
        assertEquals(map.get("k1"), "a b c");
        assertEquals(map.get("k2"), "d e f");
    }

    @Test
    @DisplayName("Test Convert Null Map")
    void testConvertNullMap() {
        Map<String, String> map = MapUtils.toMap(null);
        assertEquals(0, map.size());
    }

    @Test
    @DisplayName("Test Convert Empty Map")
    void testConvertEmptyMap() {
        MultivaluedMap<String, String> mm = new MultivaluedHashMap<String, String>();
        Map<String, String> map = MapUtils.toMap(mm);
        assertEquals(0, map.size());
    }
}
