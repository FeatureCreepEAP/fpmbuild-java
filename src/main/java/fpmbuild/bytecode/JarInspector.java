package fpmbuild.bytecode;

import fpmbuild.util.Hashing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarFile;

public final class JarInspector {
    public Map<String,Object> inspect(Path jar) throws IOException {
        long classes=0, resources=0; Map<Integer,Long> majors=new TreeMap<>(); String mainClass="";
        try(JarFile jf=new JarFile(jar.toFile())){
            if(jf.getManifest()!=null)mainClass=String.valueOf(jf.getManifest().getMainAttributes().getValue("Main-Class"));
            var es=jf.entries();while(es.hasMoreElements()){
                var e=es.nextElement(); if(e.isDirectory())continue;
                if(e.getName().endsWith(".class")){
                    classes++; try(var in=jf.getInputStream(e)){byte[] h=in.readNBytes(8);if(h.length>=8&&h[0]==(byte)0xCA&&h[1]==(byte)0xFE&&h[2]==(byte)0xBA&&h[3]==(byte)0xBE){int major=((h[6]&0xff)<<8)|(h[7]&0xff);majors.merge(major,1L,Long::sum);}}
                }else resources++;
            }
        }
        Map<String,Object> out=new LinkedHashMap<>();out.put("path",jar.toAbsolutePath().normalize().toString());out.put("sha256",Hashing.sha256(jar));out.put("classes",classes);out.put("resources",resources);out.put("classFileMajors",majors);out.put("mainClass",mainClass.equals("null")?"":mainClass);out.put("classFileApiAvailable",classFileApiAvailable());out.put("mutationSupported",false);out.put("futureBackend","java.lang.classfile (Java 25 runtime)");return out;
    }

    private static boolean classFileApiAvailable(){try{Class.forName("java.lang.classfile.ClassFile");return true;}catch(ClassNotFoundException e){return false;}}
}
