package fpmbuild;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.asbestosstar.assistremapper.Mappings;
import com.asbestosstar.assistremapper.mappings.PDMEMappings;
import com.asbestosstar.assistremapper.remapper.JarRemapper;




public class fpmbuild_remapper {

	public File fpm;
	public Mappings mappings;
	public JarRemapper remapper;
	public String path_to_fpm;
	
	public fpmbuild_remapper(String path_to_fpm, String mappings_location, String dependency_location) {
		// TODO Auto-generated constructor stub
	File fpm = new File(path_to_fpm);
	File mappings = new File(mappings_location);
	File dependency = new File(dependency_location);
	this.fpm=fpm;
	this.path_to_fpm=path_to_fpm;
	try {
		this.mappings=new PDMEMappings(new FileInputStream(mappings));//Need to soon allow for other mappings
		String run_dir = System.getProperty("user.dir");
		this.remapper = new JarRemapper(this.mappings,run_dir+"/BUILD_ROOT/");
		
		//get all the files in the dependency folder
		for(File dep: dependency.listFiles())
		{
			remapper.addToClasspathJar(new JarFile(dep),false);
		}
		
		
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	
	
	}
	
	public void mapInPlace() {
try {
	String run_dir = System.getProperty("user.dir");
	String br = new String(run_dir+"/BUILD_ROOT/");
File build_root= new File(br);

extractFilesFromZip(path_to_fpm,br);
File unmapped = new File(fpm.getAbsoluteFile()+".unmapped.jar");	
fpm.renameTo(unmapped);//Need to update mapper to work on nonjars
remapper.remapJar(new JarFile(unmapped));

try {
         FileOutputStream fos = new FileOutputStream(fpm);
         ZipOutputStream zos = new ZipOutputStream(fos);

         zipFolder(build_root, br, zos);

         zos.close();
         fos.close();

         System.out.println("Files zipped successfully");
     } catch (IOException e) {
         e.printStackTrace();
     }
	
		for(File file: build_root.listFiles()) {
		file.deleteOnExit();	
		}
	
	
} catch (IOException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}

	}
	
	
	
	
public static void zipFolder(File folder, String superFolder, ZipOutputStream zos) throws IOException {
    Set<String> entryNames = new HashSet<>();
    
    for (File file : folder.listFiles()) {
        if (file.isDirectory()) {
            zipFolder(file, superFolder, zos);
//            zipFolder(file, file.getName(), zos);

            //            zipFolder(file, superFolder + "/" + file.getName(), zos);

        } else {
            byte[] buffer = new byte[1024];
            FileInputStream fis = new FileInputStream(file);
            // Get the relative path of the current file with respect to the superFolder

            String relativePath = new String(folder+"/"+file.getName()).replace(superFolder, "");
            System.out.println(relativePath);
            String entryName = relativePath;
            int i = 1;
            while (entryNames.contains(entryName)) {
                int extensionIndex = relativePath.lastIndexOf('.');
                if (extensionIndex != -1) {
                    entryName = relativePath.substring(0, extensionIndex) + "_" + i + relativePath.substring(extensionIndex);
                } else {
                    entryName = relativePath + "_" + i;
                }
                i++;
            }
            
            entryNames.add(entryName);
            zos.putNextEntry(new ZipEntry(entryName));

            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
            fis.close();
        }
    }
}	






public static void extractFilesFromZip(String zipFilePath, String destinationFolderPath) {
    try (ZipFile zipFile = new ZipFile(zipFilePath)) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String entryName = entry.getName();
            File destFile = new File(destinationFolderPath + entryName);
            
            // Create destination folder if it does not exist
            new File(destFile.getParent()).mkdirs();

            if (!entry.isDirectory()) {
                extractFile(zipFile, entry, destFile);
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}

private static void extractFile(ZipFile zipFile, ZipEntry entry, File destFile) throws IOException {
    try (InputStream is = zipFile.getInputStream(entry);
         FileOutputStream fos = new FileOutputStream(destFile)) {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }
    }
}




}

	


