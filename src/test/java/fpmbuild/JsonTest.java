package fpmbuild;

import fpmbuild.json.Json;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class JsonTest {
    @Test void roundTripsAgentObject() {
        String s=Json.stringify(Json.object("id",1,"command","inspect-project","args",java.util.List.of("--path","/tmp/x")));
        Map<String,Object> m=Json.asObject(Json.parse(s));
        assertEquals("inspect-project",m.get("command"));
    }
}
