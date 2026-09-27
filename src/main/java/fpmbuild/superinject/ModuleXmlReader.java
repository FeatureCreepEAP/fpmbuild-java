package fpmbuild.superinject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class ModuleXmlReader {
    private static final List<String> MIXIN_KEYS = List.of("MixinConfigs", "SpongeMixinConfig", "FeatureCreep-Mixin-Configs");

    ModuleMetadata read(String entryName, byte[] xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        trySet(f, "http://apache.org/xml/features/disallow-doctype-decl", true);
        trySet(f, "http://xml.org/sax/features/external-general-entities", false);
        trySet(f, "http://xml.org/sax/features/external-parameter-entities", false);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        Document document = f.newDocumentBuilder().parse(new InputSource(new StringReader(new String(xml, java.nio.charset.StandardCharsets.UTF_8))));

        Element root = document.getDocumentElement();
        String moduleName = root == null ? "" : root.getAttribute("name").trim();

        Map<String,String> properties = new LinkedHashMap<>();
        NodeList all = document.getElementsByTagNameNS("*", "property");
        if (all.getLength() == 0) all = document.getElementsByTagName("property");
        for (int i=0;i<all.getLength();i++) {
            Node n = all.item(i);
            if (!(n instanceof Element e)) continue;
            String name = e.getAttribute("name");
            String value = e.getAttribute("value");
            if (!name.isBlank()) properties.put(name, value);
        }

        String explicitModId = trimToEmpty(getIgnoreCase(properties, "FeatureCreep-Mod-Id"));
        String chosenModId = !explicitModId.isBlank() ? explicitModId : moduleName;
        String source = !explicitModId.isBlank() ? "FeatureCreep-Mod-Id" : !moduleName.isBlank() ? "module-name" : "";

        Set<String> mixins = new LinkedHashSet<>();
        for (String key : MIXIN_KEYS) splitList(getIgnoreCase(properties, key), mixins);
        return new ModuleMetadata(entryName, moduleName, chosenModId, ModId.normalize(chosenModId), source, new ArrayList<>(mixins));
    }

    private static void trySet(DocumentBuilderFactory f, String feature, boolean value) {
        try { f.setFeature(feature, value); } catch (Exception ignored) { }
    }

    private static String getIgnoreCase(Map<String,String> map, String key) {
        String direct = map.get(key);
        if (direct != null) return direct;
        String lower = key.toLowerCase(Locale.ROOT);
        for (var e : map.entrySet()) if (e.getKey().toLowerCase(Locale.ROOT).equals(lower)) return e.getValue();
        return null;
    }

    private static void splitList(String value, Set<String> out) {
        if (value == null || value.isBlank()) return;
        for (String s : value.split("[,;\\s]+")) if (!s.isBlank()) out.add(s.trim());
    }

    private static String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
}
