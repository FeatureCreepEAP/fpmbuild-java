package fpmbuild;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgentApiTest {
    @Test void capabilitiesAreMachineReadable() throws Exception {
        var result=Main.executeForAgent(new String[]{"capabilities"});
        assertEquals("JSON Lines over stdin/stdout",result.get("agentProtocol"));
        assertEquals(25,result.get("requiredJava"));
    }
}
